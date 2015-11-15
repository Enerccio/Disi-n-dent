package cz.upol.vanusanik.disindent.compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.FieldSignatures;
import cz.upol.vanusanik.disindent.buildpath.FunctionSignature.SignatureSpecifier;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.buildpath.FunctionSignatures;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.errors.CompilationException;
import cz.upol.vanusanik.disindent.errors.MalformedImportDeclarationException;
import cz.upol.vanusanik.disindent.errors.TypeException;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;
import cz.upol.vanusanik.disindent.utils.Utils;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.AssignmentContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.AssignmentsContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.AtomContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.BlockContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ConstArgContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Field_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqModuleNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FunctionContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.HeaderContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.IdentifierContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.NativeImportContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.OperationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ParameterContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypedefContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.UsesContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_functionsContext;

/**
 * DisindentCompiler compiles disindent (din) files into java classes
 * 
 * @author Peter Vanusanik
 *
 */
public class DisindentCompiler implements Opcodes {

	/** parser instance */
	private disindentParser parser;
	/** filename of the input string, module name is created from it */
	private String filename;
	/** name of the module being compiled, each din file is one module */
	private String moduleName;
	/** bound class loader */
	private DisindentClassLoader cl;
	/** imports are listed here */
	private Imports imports = new Imports();

	public DisindentCompiler(String filename, disindentParser parser,
			DisindentClassLoader loader) {
		this.parser = parser;
		this.filename = filename;
		this.moduleName = FilenameUtils.getBaseName(filename);
		this.cl = loader;
	}

	/** Package name of this module */
	private String packageName = "";
	/** Native package of this module */
	private String nativePackage = "";
	/** Function context for actively compiled function */
	private cz.upol.vanusanik.disindent.compiler.FunctionContext fc;
	/** */
	private String fqThisType;

	/**
	 * Commence the compilation of the loaded module. Will return classes
	 * created in this module via CompilationResult
	 * 
	 * @return all java classes compiled from this module
	 * @throws CompilationException
	 *             if anything goes wrong
	 */
	public void compile() throws CompilationException {
		try {
			compileModule();
		} catch (CompilationException e) {
			throw e;
		} catch (Throwable t) {
			throw new CompilationException(t);
		}
	}

	/**
	 * Compiles module in this compiler and adds it into compilation result
	 */
	private void compileModule() {
		byte[] moduleByteData = doCompileModule();

		cl.addClass(
				Utils.slashify(Utils.fullNameForClass(
						Utils.asModuledefJavaName(moduleName), packageName)),
				moduleByteData);
	}

	/**
	 * Compiles module into java class as byte array
	 * 
	 * @return
	 */
	private byte[] doCompileModule() {

		ProgramContext pc = parser.program();
		if (pc.package_declaration() != null) {
			packageName = pc.package_declaration().javaName().getText();
			fqThisType = Utils.slashify(packageName) + "/" + Utils.asModuledefJavaName(moduleName);
		} else {
			fqThisType = Utils.asModuledefJavaName(moduleName);
		}

		if (pc.native_declaration() != null) {
			nativePackage = pc.native_declaration().javaName().getText();
		}

		if (pc.using_declaration().size() != 0) {
			resolveImports(pc);
		}

		for (TypedefContext tdc : pc.typedef()) {
			compileNewType(tdc);
		}

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
				fqThisType, null,
				"java/lang/Object", null);
		cw.visitSource(filename, null);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
				null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(0, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
				false);
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", Utils.asLName(fqThisType), null, l0, l1,
				0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		for (NativeImportContext nc : pc.nativeImport()) {
			compileNative(cw, nc);
		}

		for (FunctionContext fc : pc.function()) {
			compileFunction(cw, fc);
		}

		cw.visitEnd();

		byte[] classData = cw.toByteArray();

		PrintWriter pw = new PrintWriter(System.out);
		try {
			CheckClassAdapter.verify(new ClassReader(classData), cl, true, pw);
		} catch (Throwable t){
			t.printStackTrace();
		}

		return classData;
	}

	/**
	 * Resolves the identifier access to other functions/modules
	 * 
	 * @param pd
	 */
	private void resolveImports(ProgramContext pc) {
		for (Using_declarationContext ud : pc.using_declaration()) {
			if (ud.using_module() != null) {
				String fqName = ud.using_module().fqNameImport().getText();
				imports.addImport(fqName);
			} else {
				Using_functionsContext fc = ud.using_functions();
				List<UsesContext> usesList = Utils.searchForElementOfType(
						UsesContext.class, fc);
				FqModuleNameContext fmnc = Utils
						.searchForElementOfType(FqModuleNameContext.class, fc)
						.iterator().next();

				String moduleName = fmnc.getText();
				for (UsesContext usc : usesList) {
					for (IdentifierContext i : usc.identifier()) {
						imports.addImport(moduleName, i.getText());
					}
				}
			}
		}

		for (TypedefContext tc : pc.typedef()) {
			String typedefName = tc.typedef_header().identifier().getText();
			String fqName = (packageName.equals("") ? moduleName : packageName
					+ "." + moduleName)
					+ "." + typedefName;
			imports.addImport(fqName, typedefName);
		}

		for (FunctionContext fc : pc.function()) {
			String fqName = packageName.equals("") ? moduleName : packageName
					+ "." + moduleName;
			imports.addImport(fqName, fc.header().identifier().getText());
		}

		for (NativeImportContext nc : pc.nativeImport()) {
			String fqName = packageName.equals("") ? moduleName : packageName
					+ "." + moduleName;
			imports.addImport(fqName, nc.identifier().getText());
		}
	}

	/**
	 * Compiles new type
	 * 
	 * @param tdc
	 */
	private void compileNewType(TypedefContext tdc) {
		String typedefName = tdc.typedef_header().identifier().getText();
		String fqTypedefPath = (packageName.equals("") ? moduleName : packageName
				+ "." + moduleName) + "." + Utils.asTypedefJavaName(typedefName);
		String fqJavaPath = fqThisType + "$" + Utils.asTypedefJavaName(typedefName);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER,
				fqJavaPath, null,
				"java/lang/Object", null);
		cw.visitSource(filename, null);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
				null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(0, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
				false);
		
		fc = new cz.upol.vanusanik.disindent.compiler.FunctionContext(null);
		fc.push();
		TypeRepresentation thisType = new TypeRepresentation();
		thisType.setFqTypeName(fqTypedefPath);
		fc.addLocal("this", thisType);
		
		for (Field_declarationContext fdc : tdc.typedef_body().field_declaration()){
			String fname = fdc.identifier().getText();
			TypeRepresentation type = CompilerUtils.asType(fdc.type(), imports);
			
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, fname, type.toJVMTypeString(), null, null);
			fv.visitEnd();
			
			mv.visitVarInsn(ALOAD, 0);
			if (fdc.atom() != null){
				TypeRepresentation atomType = compileAtom(mv, fdc.atom(), true);
				if (!CompilerUtils.congruentType(atomType, type))
					throw new TypeException("wrong type at line " + fdc.start.getLine());
			} else {
				// default value
				CompilerUtils.defaultValue(mv, type);
			}
			mv.visitFieldInsn(PUTFIELD, fqJavaPath, fname, type.toJVMTypeString());
		}
		
		mv.visitInsn(RETURN);
		fc.pop(mv, l0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		cw.visitEnd();

		byte[] classData = cw.toByteArray();

		PrintWriter pw = new PrintWriter(System.out);
		try {
			CheckClassAdapter.verify(new ClassReader(classData), cl, true, pw);
		} catch (Throwable t){
			t.printStackTrace();
		}
		
		cl.addClass(fqJavaPath,	classData);
	}

	/**
	 * Compiles native call
	 * 
	 * @param cw
	 * @param nc
	 */
	private void compileNative(ClassWriter cw, NativeImportContext nc) {
		String className = moduleName;
		String fqType = Utils.slashify(nativePackage.equals("") ? className
				: nativePackage + "." + className);

		List<TypeRepresentation> typeList = new ArrayList<TypeRepresentation>();
		TypeRepresentation retType = CompilerUtils.asType(nc.type(), imports);

		typeList.add(retType);
		fc = new cz.upol.vanusanik.disindent.compiler.FunctionContext(retType);
		fc.push();

		for (ParameterContext pc : Utils.searchForElementOfType(
				ParameterContext.class, nc.func_arguments())) {
			TypeRepresentation tr;

			fc.addLocal(pc.identifier().getText(), tr = CompilerUtils.asType(pc.type(), imports));
			typeList.add(tr);
		}

		FunctionSignatures fcs = BuildPath.getBuildPath().getSignatures(
				packageName, moduleName, nc.identifier().getText());
		SignatureSpecifier sign = fcs.getSpecifier(nc.identifier().getText(),
				typeList);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, nc.identifier().getText(), sign.javaSignature, null,
				null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLineNumber(nc.start.getLine(), start);
		
		for (ParameterContext pc : Utils.searchForElementOfType(
				ParameterContext.class, nc.func_arguments())) {
			String auto = pc.identifier().getText();
			Integer id = fc.autoToNum(auto);
			TypeRepresentation type = fc.autoToType(auto);
			
			CompilerUtils.addLoad(mv, id, type);
		}
		
		mv.visitMethodInsn(INVOKESTATIC, fqType, nc.identifier().getText(), CompilerUtils.nativeSignature(typeList), false);
		CompilerUtils.addReturn(mv, fc.returnType);
		
		fc.pop(mv, start);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	

	/**
	 * Compiles new function to this module
	 * 
	 * @param cw
	 * @param fc
	 */
	private void compileFunction(ClassWriter cw, FunctionContext fc) {
		HeaderContext header = fc.header();
		BlockContext body = fc.block();
		
		List<TypeRepresentation> typeList = new ArrayList<TypeRepresentation>();
		TypeRepresentation retType = CompilerUtils.asType(header.type(), imports);

		typeList.add(retType);
		this.fc = new cz.upol.vanusanik.disindent.compiler.FunctionContext(retType);
		this.fc.push();

		for (ParameterContext pc : Utils.searchForElementOfType(
				ParameterContext.class, header.func_arguments())) {
			TypeRepresentation tr;

			this.fc.addLocal(pc.identifier().getText(), tr = CompilerUtils.asType(pc.type(), imports));
			typeList.add(tr);
		}

		FunctionSignatures fcs = BuildPath.getBuildPath().getSignatures(
				packageName, moduleName, header.identifier().getText());
		SignatureSpecifier sign = fcs.getSpecifier(header.identifier().getText(),
				typeList);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, header.identifier().getText(), sign.javaSignature, null,
				null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLineNumber(header.start.getLine(), start);
		
		compileBody(mv, body);
		
		this.fc.pop(mv, start);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Compiles body element.
	 * 
	 * Body is compiled operation by operation and last operation's value is returned
	 * @param mv
	 * @param body
	 */
	private void compileBody(MethodVisitor mv, BlockContext body) {
		Label bodyLabel = new Label();
		mv.visitLabel(bodyLabel);
		mv.visitLineNumber(body.start.getLine(), bodyLabel);
		
		int opc = body.operation().size();
		for (int i=0; i<opc-1; i++)
			compileOperation(mv, body.operation(i), false);
		compileOperation(mv, body.operation(opc-1), true);
	}

	/**
	 * Compiles single operation
	 * @param mv
	 * @param operation
	 * @param last whether the operation is last on stack 
	 */
	private void compileOperation(MethodVisitor mv, OperationContext operation,
			boolean last) {
		Label opLabel = new Label();
		mv.visitLabel(opLabel);
		mv.visitLineNumber(operation.start.getLine(), opLabel);
		
		TypeRepresentation ret = null;
		
		if (operation.head() == null){
			ret = compileAtom(mv, operation.atom(), last);
		} else {
			
		}
		
		if (last){
			if (!CompilerUtils.congruentType(ret, fc.returnType))
				throw new TypeException("wrong type at line " + operation.start.getLine());
			CompilerUtils.addReturn(mv, fc.returnType);
		}
	}

	/**
	 * Compiles atom, returns atom's type as type
	 * @param mv
	 * @param atom
	 * @param last 
	 * @return
	 */
	private TypeRepresentation compileAtom(MethodVisitor mv, AtomContext atom, boolean last) {
		if (!last)
			return null; // atoms are ignored if not last
		
		Label opLabel = new Label();
		mv.visitLabel(opLabel);
		mv.visitLineNumber(atom.start.getLine(), opLabel);
		
		if (atom.getText().equals("none")){
			// null
			// returns null type
			mv.visitInsn(ACONST_NULL);
			return TypeRepresentation.NULL;
		}
		
		if (atom.getText().equals("true")){
			// true
			// returns bool type
			mv.visitInsn(ICONST_1);
			return TypeRepresentation.BOOL;
		}
		
		if (atom.getText().equals("false")){
			// false
			// returns bool type
			mv.visitInsn(ICONST_0);
			return TypeRepresentation.BOOL;
		}
		
		if (atom.cast() != null){
			// cast operation
			// returns cast type
			TypeRepresentation tr = compileAtom(mv, atom.cast().atom(), last);
			TypeRepresentation as = CompilerUtils.asType(atom.cast().type(), imports);
			CompilerUtils.congruentCast(mv, tr, as);
			return as;
		}
		
		if (atom.accessor() != null){
			// accessor operation
			// return type of last nested accessor
			TypeRepresentation varType = null;
			for (IdentifierContext i : atom.accessor().dottedName().identifier()){
				String name = i.getText();
				if (varType == null){
					varType = fc.autoToType(name);
					CompilerUtils.addLoad(mv, fc.autoToNum(name), varType);
				} else {
					if (!varType.isCustomType())
						throw new TypeException("Object is not typedef at line " + i.start.getLine());
					FieldSignatures fs = BuildPath.getBuildPath().getFieldSignatures(varType.getFqTypeName());
					TypeRepresentation nvarType = fs.getType(name);
					mv.visitFieldInsn(GETFIELD, Utils.slashify(varType.getFqTypeName()), name, nvarType.toJVMTypeString());
					varType = nvarType;
				}
			}
			return varType;
		}
		
		if (atom.constArg() != null){
			// constant
			ConstArgContext c = atom.constArg();
			
			if (c.IntegerConstant() != null){
				// integer
				String text = c.getText();
				
				int iv;
				if (text.length() == 1)
					iv = Integer.parseInt(text);
				else if (text.charAt(0) == '0' && text.contains("b"))
					iv = Integer.parseInt(text.substring(2), 2);
				else if (text.charAt(0) == '0' && text.contains("x"))
					iv = Integer.parseInt(text.substring(2), 16);
				else if (text.charAt(0) == '0')
					iv = Integer.parseInt(text.substring(1), 8);
				else
					iv = Integer.parseInt(text);
				
				mv.visitLdcInsn(iv);
				return TypeRepresentation.INT;
			}
			
			if (c.LongConstant() != null){
				// long
				String text = c.getText();
				text = text.substring(0, text.length()-1);
				
				long lv;
				if (text.length() == 1)
					lv = Long.parseLong(text);
				else if (text.charAt(0) == '0' && text.contains("b"))
					lv = Long.parseLong(text.substring(2), 2);
				else if (text.charAt(0) == '0' && text.contains("x"))
					lv = Long.parseLong(text.substring(2), 16);
				else if (text.charAt(0) == '0')
					lv = Long.parseLong(text.substring(1), 8);
				else
					lv = Long.parseLong(text);
				
				mv.visitLdcInsn(lv);
				return TypeRepresentation.LONG;
			}
			
			if (c.DoubleConstant() != null){
				// double
				String text = c.getText();
				double dv = Double.parseDouble(text);
				mv.visitLdcInsn(dv);
				return TypeRepresentation.DOUBLE;
			}
			
			if (c.FloatConstant() != null){
				// float
				String text = c.getText();
				text = text.substring(0, text.length()-1);
				float fv = Float.parseFloat(text);
				mv.visitLdcInsn(fv);
				return TypeRepresentation.FLOAT;
			}
			
			if (c.String() != null){
				// string
				String strValue = c.getText();
				strValue = Utils.removeEscapes(strValue);
				strValue = strValue.substring(1, strValue.length()-1);
				mv.visitLdcInsn(strValue);
				return TypeRepresentation.STRING;
			}
			
			if (c.make() != null || c.clone() != null){
				// clone or assign
				boolean clone = c.clone() != null;
				List<AssignmentsContext> ac = clone ? c.clone().assignments() : c.make().assignments();
				FqNameContext typeFQName = clone ? c.clone().fqName() : c.make().fqName();
				
				String tt = typeFQName.getText();
				String fqPath = imports.importMapOriginal.get(tt);
				if (fqPath == null)
					throw new MalformedImportDeclarationException("type " + tt
							+ " is not defined");

				TypeRepresentation tr = new TypeRepresentation();
				tr.setType(SystemTypes.CUSTOM);
				tr.setFqTypeName(fqPath);
				
				mv.visitTypeInsn(NEW, tr.getJavaClassName());
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, tr.getJavaClassName(), "<init>", "()V", false);
				
				FieldSignatures fs = BuildPath.getBuildPath().getFieldSignatures(tr.getFqTypeName());
				
				// clone fields
				if (clone){
					TypeRepresentation cloneType = compileAtom(mv, c.clone().atom(), true);
					if (!CompilerUtils.congruentType(tr, cloneType))
						throw new TypeException("wrong type at line " + c.clone().start.getLine());
					
					for (String fname : fs.getFields()){
						mv.visitInsn(DUP2);
						TypeRepresentation varType = fs.getType(fname);
						mv.visitFieldInsn(GETFIELD, tr.getJavaClassName(), fname, varType.toJVMTypeString());
						mv.visitFieldInsn(PUTFIELD, tr.getJavaClassName(), fname, varType.toJVMTypeString());
					}
					
					mv.visitInsn(POP);
				}
				
				// assign fields
				for (AssignmentsContext a : ac){
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitLineNumber(a.start.getLine(), l0);
					
					for (AssignmentContext assign : a.assignment()){
						mv.visitInsn(DUP);
						String fname = assign.identifier().getText();
						TypeRepresentation varType = fs.getType(fname);
						TypeRepresentation actualType = compileAtom(mv, assign.atom(), true);
						if (!CompilerUtils.congruentType(varType, actualType))
							throw new TypeException("wrong type at line " + assign.start.getLine());
						mv.visitFieldInsn(PUTFIELD, tr.getJavaClassName(), fname, varType.toJVMTypeString());
					}
				}
				
				return tr;
			}
			
			if (c.funcptr() != null){
				// function pointer
				String fncName = c.funcptr().funcdesignator().getText();
				String fqPath = imports.importMapOriginal.get(fncName);
				if (fqPath == null)
					throw new MalformedImportDeclarationException("function " + fncName
							+ " is not defined");
				String typeName = fqPath.substring(0, fqPath.lastIndexOf('.'));
				String cp = BuildPath.getBuildPath().getClassPath(typeName);
				
				mv.visitLdcInsn(fncName);
				mv.visitLdcInsn(Type.getType(Utils.asLName(fqThisType)));
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitLdcInsn(cp);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				mv.visitMethodInsn(INVOKESTATIC, "cz/upol/vanusanik/disindent/runtime/types/Method", "makeFunction", "(Ljava/lang/String;Ljava/lang/Class;)Lcz/upol/vanusanik/disindent/runtime/types/Method;", false);
				return TypeRepresentation.FUNCTION;
			}
		}
		
		if (atom.constList() != null){
			// list type
			TypeRepresentation st = CompilerUtils.asType(atom.constList().type(), imports);
			TypeRepresentation tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(st);
			
			if (atom.constList().atoms() != null){
				for (AtomContext a : atom.constList().atoms().atom()){
					TypeRepresentation atomType = compileAtom(mv, a, true);
					if (!CompilerUtils.congruentType(atomType, st))
						throw new TypeException("wrong type at line " + a.start.getLine());
				}
				
				mv.visitInsn(ACONST_NULL);
				
				for (int i=0; i<atom.constList().atoms().atom().size(); i++){
					mv.visitMethodInsn(INVOKESTATIC, "cz/upol/vanusanik/disindent/runtime/types/DList", "constList", 
							"(Ljava/lang/Object;Lcz/upol/vanusanik/disindent/runtime/types/DList;)Lcz/upol/vanusanik/disindent/runtime/types/DList;", false);
				}
			}
			
			return tr;
		}
		
		return null;
	}
}
