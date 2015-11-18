package cz.upol.vanusanik.disindent.compiler;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
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
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Complex_operationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ConstArgContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Field_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.For_operationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqModuleNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FunctionContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.HeadContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.HeaderContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.IdentifierContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.If_operationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.NativeImportContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.OperationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ParameterContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Simple_opContext;
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
	/** fully qualified java type name */
	private String fqThisType;
	/** used for creating tempvars */
	private int gensymCount = 0;
	/** stack for loops */
	private Stack<Boolean> forLoopStack = new Stack<Boolean>();

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
			fqThisType = Utils.slashify(packageName) + "/"
					+ Utils.asModuledefJavaName(moduleName);
		} else {
			fqThisType = Utils.asModuledefJavaName(moduleName);
		}

		if (pc.native_declaration() != null) {
			nativePackage = pc.native_declaration().javaName().getText();
		}

		resolveImports(pc);

		for (TypedefContext tdc : pc.typedef()) {
			compileNewType(tdc);
		}

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
				| ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, fqThisType, null,
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
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		exportToTmp(classData, fqThisType);

		return classData;
	}

	/**
	 * Resolves the identifier access to other functions/modules
	 * 
	 * @param pd
	 */
	private void resolveImports(ProgramContext pc) {
		imports.addImport("System", true);
		
		for (Using_declarationContext ud : pc.using_declaration()) {
			if (ud.using_module() != null) {
				String fqName = ud.using_module().fqNameImport().getText();
				imports.addImport(fqName, false);
			} else {
				Using_functionsContext fc = ud.using_functions();
				List<UsesContext> usesList = Utils.searchForElementOfType(
						UsesContext.class, fc);
				FqModuleNameContext fmnc = Utils
						.searchForElementOfType(FqModuleNameContext.class, fc)
						.iterator().next();

				moduleName = fmnc.getText();
				for (UsesContext usc : usesList) {
					for (IdentifierContext i : usc.identifier()) {
						imports.addImport(moduleName, i.getText(), false);
					}
				}
			}
		}

		for (TypedefContext tc : pc.typedef()) {
			String typedefName = tc.typedef_header().identifier().getText();
			String fqName = (packageName.equals("") ? moduleName : packageName
					+ "." + moduleName)
					+ "." + typedefName;
			imports.add(typedefName, fqName);
			imports.add(moduleName + "." + typedefName, fqName);
		}

		for (FunctionContext fc : pc.function()) {
			String fncName = fc.header().identifier().getText();
			String fqName = (packageName.equals("") ? moduleName : packageName
					+ "." + moduleName)
					+ "." + fncName;
			imports.add(fncName, fqName);
			imports.add(moduleName + "." + fncName, fqName);
		}

		for (NativeImportContext nc : pc.nativeImport()) {
			String fncName = nc.identifier().getText();
			String fqName = (packageName.equals("") ? moduleName : packageName
					+ "." + moduleName + ".")
					+ fncName;
			imports.add(fncName, fqName);
			imports.add(moduleName + "." + fncName, fqName);
		}
	}

	/**
	 * Compiles new type
	 * 
	 * @param tdc
	 */
	private void compileNewType(TypedefContext tdc) {
		String typedefName = tdc.typedef_header().identifier().getText();
		String fqTypedefPath = (packageName.equals("") ? moduleName
				: packageName + "." + moduleName)
				+ "."
				+ Utils.asTypedefJavaName(typedefName);
		String fqJavaPath = fqThisType + "$"
				+ Utils.asTypedefJavaName(typedefName);

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
				| ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, fqJavaPath, null,
				"java/lang/Object", new String[]{"java/io/Serializable"});
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

		for (Field_declarationContext fdc : tdc.typedef_body()
				.field_declaration()) {
			String fname = fdc.identifier().getText();
			TypeRepresentation type = CompilerUtils.asType(fdc.type(), imports);

			FieldVisitor fv = cw.visitField(ACC_PUBLIC, fname,
					type.toJVMTypeString(), null, null);
			fv.visitEnd();

			mv.visitVarInsn(ALOAD, 0);
			if (fdc.atom() != null) {
				TypeRepresentation atomType = compileAtom(mv, fdc.atom(), true);
				if (!CompilerUtils.congruentType(mv, atomType, type))
					throw new TypeException("wrong type at line "
							+ fdc.start.getLine());
			} else {
				// default value
				CompilerUtils.defaultValue(mv, type);
			}
			mv.visitFieldInsn(PUTFIELD, fqJavaPath, fname,
					type.toJVMTypeString());
		}

		mv.visitInsn(RETURN);
		fc.pop(mv, l0, true);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cw.visitEnd();

		byte[] classData = cw.toByteArray();

		PrintWriter pw = new PrintWriter(System.out);
		try {
			CheckClassAdapter.verify(new ClassReader(classData), cl, true, pw);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		exportToTmp(classData, fqJavaPath);

		cl.addClass(fqJavaPath, classData);
	}

	private void exportToTmp(byte[] classData, String fqName) {
		File f = new File("tmp");
		if (f.exists()){
			File expF = new File(f, fqName+".class");
			expF.getParentFile().mkdirs();
			try {
				FileUtils.writeByteArrayToFile(expF, classData);
			} catch (Exception e){
				
			}
		}
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

			fc.addLocal(pc.identifier().getText(),
					tr = CompilerUtils.asType(pc.type(), imports));
			typeList.add(tr);
		}

		FunctionSignatures fcs = BuildPath.getBuildPath().getSignatures(
				packageName, moduleName);
		SignatureSpecifier sign = fcs.getSpecifier(nc.identifier().getText(),
				typeList);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, nc
				.identifier().getText(), sign.javaSignature, null, null);
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

		mv.visitMethodInsn(INVOKESTATIC, fqType, nc.identifier().getText(),
				CompilerUtils.nativeSignature(typeList), false);
		CompilerUtils.addReturn(mv, fc.returnType);

		fc.pop(mv, start, true);
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
		TypeRepresentation retType = CompilerUtils.asType(header.type(),
				imports);

		typeList.add(retType);
		this.fc = new cz.upol.vanusanik.disindent.compiler.FunctionContext(
				retType);
		this.fc.push();

		for (ParameterContext pc : Utils.searchForElementOfType(
				ParameterContext.class, header.func_arguments())) {
			TypeRepresentation tr;

			this.fc.addLocal(pc.identifier().getText(),
					tr = CompilerUtils.asType(pc.type(), imports));
			typeList.add(tr);
		}

		FunctionSignatures fcs = BuildPath.getBuildPath().getSignatures(
				packageName, moduleName);
		SignatureSpecifier sign = fcs.getSpecifier(header.identifier()
				.getText(), typeList);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, header
				.identifier().getText(), sign.javaSignature, null, null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLineNumber(header.start.getLine(), start);

		forLoopStack.push(false);
		TypeRepresentation ret = compileBlock(mv, body);
		forLoopStack.pop();
		
		if (!CompilerUtils.congruentType(mv, ret, this.fc.returnType))
			throw new TypeException("wrong type at line "
					+ body.operation(body.operation().size() - 1).start.getLine() + " expected "
					+ this.fc.returnType + ", got " + ret);
		CompilerUtils.addReturn(mv, this.fc.returnType);

		this.fc.pop(mv, start, true);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Compiles body element.
	 * 
	 * Body is compiled operation by operation and last operation's value is
	 * returned
	 * 
	 * @param mv
	 * @param body
	 * @return 
	 */
	private TypeRepresentation compileBlock(MethodVisitor mv, BlockContext body) {
		Label blockLabel = new Label();
		mv.visitLabel(blockLabel);
		mv.visitLineNumber(body.start.getLine(), blockLabel);
		
		fc.push();
		int opc = body.operation().size();
		for (int i = 0; i < opc - 1; i++)
			compileOperation(mv, body.operation(i), false);
		TypeRepresentation ret = compileOperation(mv, body.operation(opc - 1),
				true);
		fc.pop(mv, blockLabel, !forLoopStack.peek());
		
		return ret;
	}

	/**
	 * Compiles single operation
	 * 
	 * @param mv
	 * @param operation
	 * @param last
	 *            whether the operation is last on stack
	 */
	private TypeRepresentation compileOperation(MethodVisitor mv,
			OperationContext operation, boolean last) {
		Label opLabel = new Label();
		mv.visitLabel(opLabel);
		mv.visitLineNumber(operation.start.getLine(), opLabel);

		TypeRepresentation ret = null;

		if (operation.head() == null) {
			// TODO specific forms added later like if here
			if (operation.complex_operation() != null) {
				return compileComplexOperation(mv,
						operation.complex_operation(), last);
			}

			ret = compileAtom(mv, operation.atom_operation().atom(), last);
			if (!last) {
				if (ret.isDoubleMemory())
					mv.visitInsn(POP2);
				else
					mv.visitInsn(POP);
			}
		} else {
			ret = compileOp(mv, operation, last);
			if (!last)
				if (ret.isDoubleMemory())
					mv.visitInsn(POP2);
				else
					mv.visitInsn(POP);
		}

		return ret;
	}

	/**
	 * Compiles complex operation element (if, etc)
	 * 
	 * @param mv
	 * @param complex_operation
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileComplexOperation(MethodVisitor mv,
			Complex_operationContext complex_operation, boolean last) {

		if (complex_operation.if_operation() != null)
			return compileIf(mv, complex_operation.if_operation(), last);
		if (complex_operation.for_operation() != null)
			return compileFor(mv, complex_operation.for_operation(), last);
		
		return null;
	}

	private TypeRepresentation compileFor(MethodVisitor mv,
			For_operationContext fo, boolean last) {
		fc.push();
		
		Label loopStart = new Label();
		Label forInit = new Label();
		Label bodyLabel = new Label();
		Label forEndOfComp = new Label();
		Label forTest = new Label();
		Label forMod = new Label();
		Label forEnd = new Label();
		String operation = "";
		String tempvar1 = "for$" + gensymCount++;
		String tempvar2 = "for$" + gensymCount++;
		TypeRepresentation ret = null;
		
		AtomContext initIterator = fo.atom(0);
		AtomContext testIterator = fo.atom(1);
		AtomContext modIterator  = fo.atom(2);
		
		if (fo.identifier() != null)
			operation = fo.identifier().getText();
		
		// compile local bound variable available in body
		TypeRepresentation iteratorType = CompilerUtils.asType(fo.parameter().type(), imports);
		String iterator = fo.parameter().identifier().getText();
		fc.addLocal(iterator, iteratorType);
		
		// loop start goto
		mv.visitLabel(loopStart);
		mv.visitLineNumber(fo.start.getLine(), loopStart);
		mv.visitJumpInsn(GOTO, forInit);
		
		// loop body
		mv.visitLabel(bodyLabel);
		mv.visitLineNumber(fo.block().start.getLine(), bodyLabel);
		
		forLoopStack.push(true);
		TypeRepresentation bodyType = compileBlock(mv, fo.block());
		forLoopStack.pop();
		
		mv.visitJumpInsn(GOTO, forEndOfComp); // bodyType is on stack
		
		// loop start
		mv.visitLabel(forInit);
		mv.visitLineNumber(fo.parameter().start.getLine(), forInit);
		// initialize iterator variable
		TypeRepresentation initType = compileAtom(mv, initIterator, true);
		if (!CompilerUtils.congruentType(mv, initType, iteratorType))
			throw new TypeException("wrong type for initializer");
		CompilerUtils.addStore(mv, fc.autoToNum(iterator), iteratorType);
		// initialize end of body operation
		switch (operation){
		case "sum":
			if (!(bodyType.isNumber()) && !(bodyType.getType() == SystemTypes.STRING))
				throw new TypeException("for for sum you need a number type or string");
			fc.addLocal(tempvar1, bodyType);
			if (bodyType.isNumber()){
				CompilerUtils.defaultValue(mv, bodyType);
				CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
			} else {
				mv.visitLdcInsn("");
				CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
			}
			break;
		case "avg":
			if (!(bodyType.isNumber()))
				throw new TypeException("for for sum you need a number type or string");
			fc.addLocal(tempvar1, bodyType);
			CompilerUtils.defaultValue(mv, bodyType);
			CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
			fc.addLocal(tempvar2, TypeRepresentation.INT);
			mv.visitLdcInsn(0);
			CompilerUtils.addStore(mv, fc.autoToNum(tempvar2), TypeRepresentation.INT);
			break;
		case "app":
			// TODO
			break;
		case "rapp":
			// TODO
			break;
		case "":
		default:
			fc.addLocal(tempvar1, bodyType);
			CompilerUtils.defaultValue(mv, bodyType);
			CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
		}
		mv.visitJumpInsn(GOTO, forTest);
		
		// loop end of body
		mv.visitLabel(forEndOfComp);
		switch (operation){
		case "avg":
			mv.visitIincInsn(fc.autoToNum(tempvar2), 1);
		case "sum":
			CompilerUtils.addLoad(mv, fc.autoToNum(tempvar1), bodyType);
			List<TypeRepresentation> trL = new ArrayList<TypeRepresentation>();
			trL.add(bodyType);
			trL.add(bodyType);
			compileMath(mv, "+", trL, fo.block().start.getLine(), false);
			CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
			break;
		case "app":
			// TODO
			break;
		case "rapp":
			// TODO
			break;
		case "":
		default:
			CompilerUtils.addStore(mv, fc.autoToNum(tempvar1), bodyType);
		}
		// loop modify iterator
		mv.visitLabel(forMod);
		mv.visitLineNumber(modIterator.start.getLine(), forMod);
		TypeRepresentation modType = compileAtom(mv, modIterator, true);
		if (!CompilerUtils.congruentType(mv, modType, iteratorType))
			throw new TypeException("wrong type for iterator modificator");
		CompilerUtils.addStore(mv, fc.autoToNum(iterator), iteratorType);
		
		// loop test
		mv.visitLabel(forTest);
		mv.visitLineNumber(testIterator.start.getLine(), forTest);
		TypeRepresentation testType = compileAtom(mv, testIterator, true);
		if (!CompilerUtils.congruentType(mv, testType, TypeRepresentation.BOOL))
			throw new TypeException("for test must be bool type");
		mv.visitJumpInsn(IFEQ, forEnd); // no value on stack
		mv.visitJumpInsn(GOTO, bodyLabel);
		
		// loop end of loop
		mv.visitLabel(forEnd);
		mv.visitLineNumber(fo.stop.getLine(), forEnd);
		switch (operation){
		case "avg":
			CompilerUtils.addLoad(mv, fc.autoToNum(tempvar1), bodyType);
			CompilerUtils.addLoad(mv, fc.autoToNum(tempvar2), TypeRepresentation.INT);
			CompilerUtils.congruentCast(mv, TypeRepresentation.INT, bodyType);
			List<TypeRepresentation> trL = new ArrayList<TypeRepresentation>();
			trL.add(bodyType);
			trL.add(bodyType);
			compileMath(mv, "/", trL, fo.block().start.getLine(), false);
			ret = bodyType;
			break;
		case "sum":
			CompilerUtils.addLoad(mv, fc.autoToNum(tempvar1), bodyType);
			ret = bodyType;
			break;
		case "app":
			// TODO
			break;
		case "rapp":
			// TODO
			break;
		case "":
		default:
			CompilerUtils.addLoad(mv, fc.autoToNum(tempvar1), bodyType);
			ret = bodyType;
		}
		
		if (fo.type() != null){
			// optional cast
			TypeRepresentation as = CompilerUtils.asType(fo.type(),
					imports);
			CompilerUtils.congruentCast(mv, ret, as);
			ret = as;
		}
		
		fc.pop(mv, loopStart, !forLoopStack.peek());
		return ret;
	}

	/**
	 * Compiles if
	 * 
	 * @param mv
	 * @param if_operation
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileIf(MethodVisitor mv,
			If_operationContext if_operation, boolean last) {
		boolean hasElse = if_operation.operation().size() == 2;

		Label ifStart = new Label();
		Label ifEnd = new Label();
		Label ifElse = new Label();

		mv.visitLabel(ifStart);
		mv.visitLineNumber(if_operation.start.getLine(), ifStart);

		TypeRepresentation tr = compileAtom(mv, if_operation.atom(), true);
		if (!CompilerUtils.congruentType(mv, tr, TypeRepresentation.BOOL))
			throw new TypeException("if test expression requires bool type");
		mv.visitJumpInsn(IFEQ, hasElse ? ifElse : ifEnd);

		OperationContext trueContext = if_operation.operation(0);
		TypeRepresentation ifType = compileOperation(mv, trueContext, true);
		TypeRepresentation elseType = ifType;

		if (hasElse) {
			mv.visitJumpInsn(GOTO, ifEnd);
			mv.visitLabel(ifElse);
			elseType = compileOperation(mv, if_operation.operation(1), true);
		}

		mv.visitLabel(ifEnd);
		mv.visitLineNumber(if_operation.stop.getLine(), ifEnd);

		if (!ifType.equals(elseType))
			throw new TypeException("types must match for if/else branches");

		return ifType;
	}

	/**
	 * Compiles simple operation (call or math operation)
	 * 
	 * @param mv
	 * @param simple_op
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileSimpleOp(MethodVisitor mv,
			Simple_opContext simple_op, boolean last) {
		List<TypeRepresentation> argList = new ArrayList<TypeRepresentation>();

		if (simple_op.simple_arguments() != null) {
			List<AtomContext> aList = new ArrayList<AtomContext>(simple_op
					.simple_arguments().atom());
			for (AtomContext a : aList) {
				argList.add(compileAtom(mv, a, true));
			}
		}

		return compileStandardFuncCall(mv, argList, simple_op.head(), last);
	}

	/**
	 * Compiles operation (call or math operation)
	 * 
	 * @param mv
	 * @param operation
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileOp(MethodVisitor mv,
			OperationContext operation, boolean last) {
		List<TypeRepresentation> argList = new ArrayList<TypeRepresentation>();

		List<OperationContext> opList = new ArrayList<OperationContext>(
				operation.arguments().operation());
		for (OperationContext opc : opList) {
			TypeRepresentation tr = compileOperation(mv, opc, true);
			argList.add(tr);
		}

		TypeRepresentation retType = compileStandardFuncCall(mv, argList, operation.head(), last);
		if (operation.type() != null){
			// optional cast
			TypeRepresentation as = CompilerUtils.asType(operation.type(),
					imports);
			CompilerUtils.congruentCast(mv, retType, as);
			retType = as;
		}
		return retType;
	}

	/**
	 * Compiles standard function call
	 * @param mv
	 * @param argList
	 * @param head
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileStandardFuncCall(MethodVisitor mv,
			List<TypeRepresentation> argList, HeadContext head, boolean last) {
		TypeRepresentation returnType = null;

		if (head.mathop() == null)
			returnType = compileCall(mv, head.fqName().getText(), argList,
					head.fqName().start.getLine());
		else
			returnType = compileMath(mv, head.mathop().getText(), argList,
					head.mathop().start.getLine(),
					head.mathop().compop() != null);

		return returnType;
	}

	/**
	 * Compiles function call
	 * 
	 * @param mv
	 * @param fncName
	 * @param argList
	 * @param lineno
	 * @return
	 */
	private TypeRepresentation compileCall(MethodVisitor mv, String fncName,
			List<TypeRepresentation> argList, int lineno) {
		String fqPath = imports.importMapOriginal.get(fncName);
		if (fqPath == null && fncName.contains("."))
			fqPath = fncName;
		if (fqPath == null)
			throw new MalformedImportDeclarationException("function " + fncName
					+ " is not defined");
		String typeName = fqPath.substring(0, fqPath.lastIndexOf('.'));
		String justTypeName = typeName;
		String packageName = "";
		if (typeName.contains(".")) {
			packageName = typeName.substring(0, typeName.lastIndexOf('.'));
			justTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);
		}

		String[] split = Utils.splitByLastDot(fncName);
		fncName = split[split.length - 1];

		BuildPath bp = BuildPath.getBuildPath();
		String cp = bp.getClassPath(typeName);
		FunctionSignatures fc = bp.getSignatures(packageName, justTypeName);

		if (fc == null || cp == null)
			throw new MalformedImportDeclarationException("type " + typeName
					+ " is not defined");

		SignatureSpecifier fncSign = fc.findByParameters(fncName, argList);
		if (fncSign == null)
			throw new TypeException("function " + fncName
					+ " has no signature for types " + argList
					+ " or does not exist");

		Label lc = new Label();
		mv.visitLabel(lc);
		mv.visitLineNumber(lineno, lc);
		mv.visitMethodInsn(INVOKESTATIC, cp, fncName, fncSign.javaSignature,
				false);

		return fncSign.retType;
	}

	/**
	 * Compiles math operation
	 * 
	 * @param mv
	 * @param mathop
	 * @param argList
	 * @param lineno
	 * @param compOp
	 * @return
	 */
	private TypeRepresentation compileMath(MethodVisitor mv, String operation,
			List<TypeRepresentation> argList, int lineno, boolean compOp) {
		Label lc = new Label();
		mv.visitLabel(lc);
		mv.visitLineNumber(lineno, lc);

		for (TypeRepresentation tr : argList) {
			if ((tr == TypeRepresentation.NULL || tr.isComplexType()
					|| tr.isCustomType()
					|| tr.getType() == SystemTypes.FUNCTION
					|| tr.getType() == SystemTypes.ANY || tr.getType() == SystemTypes.BOOL)
					&& !(compOp && operation.equals("=")))
				throw new TypeException(
						"math operations only allow nonboolean primitives");
		}

		if (argList.size() == 0) {
			if (operation.equals("/") || operation.equals("-"))
				throw new CompilationException(
						"/ or - must have at least one argument");

			if (operation.equals("+")) {
				mv.visitInsn(ICONST_0);
			}
			if (operation.equals("*")) {
				mv.visitInsn(ICONST_1);
			}

			if (compOp)
				mv.visitInsn(ICONST_1);

			return TypeRepresentation.INT;
		}

		if (argList.size() == 1) {
			if (operation.equals("+")) {
				return argList.get(0);
			}

			if (operation.equals("*")) {
				return argList.get(0);
			}

			if (operation.equals("-")) {
				switch (argList.get(0).getType()) {
				case BYTE:
					mv.visitInsn(INEG);
					mv.visitInsn(I2B);
					break;
				case DOUBLE:
					mv.visitInsn(DNEG);
					break;
				case FLOAT:
					mv.visitInsn(FNEG);
					break;
				case INT:
					mv.visitInsn(INEG);
					break;
				case LONG:
					mv.visitInsn(LNEG);
					break;
				case SHORT:
					mv.visitInsn(INEG);
					mv.visitInsn(I2S);
					break;
				case STRING:
					throw new TypeException(
							"math operation other than + does not allow strings");
				default:
					throw new TypeException("");

				}
				return argList.get(0);
			}

			if (compOp) {
				if (argList.get(0).isDoubleMemory())
					mv.visitInsn(POP2);
				else
					mv.visitInsn(POP);
				mv.visitInsn(ICONST_1);
				return TypeRepresentation.BOOL;
			}

			throw new CompilationException("/ requires at least 2 arguments");
		}

		if (compOp && argList.size() > 2)
			throw new CompilationException(
					"comparison requires 0 1 or 2 arguments");

		return doCompileMath(mv, operation, argList, compOp);
	}

	private TypeRepresentation doCompileMath(MethodVisitor mv,
			String operation, List<TypeRepresentation> argList, boolean compOp) {
		if (argList.size() == 1)
			return argList.get(0);

		TypeRepresentation val1 = argList.get(argList.size() - 1);
		TypeRepresentation val2 = argList.get(argList.size() - 2);

		if (val1.getType() == SystemTypes.STRING) {
			if (!operation.equals("+"))
				throw new TypeException(
						"math operation other than + does not allow strings");
			if (val2.getType() != SystemTypes.STRING)
				throw new TypeException("all arguments must be type of string");
			mv.visitMethodInsn(INVOKESTATIC,
					"cz/upol/vanusanik/disindent/runtime/types/StringTools",
					"concat",
					"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
					false);
			return TypeRepresentation.STRING;
		}

		switch (val1.getType()) {
		case BYTE:
		case SHORT:
		case INT:
			switch (val2.getType()) {

			case BYTE:
				mv.visitInsn(I2B);
				break;
			case SHORT:
				mv.visitInsn(I2S);
				break;
			case INT:
				// no action
				break;
			case LONG:
				mv.visitInsn(I2L);
				break;
			case DOUBLE:
				mv.visitInsn(I2D);
				break;
			case FLOAT:
				mv.visitInsn(I2F);
				break;
			default:
				break;
			}
			break;
		case LONG:
			switch (val2.getType()) {
			case BYTE:
				mv.visitInsn(L2I);
				mv.visitInsn(I2B);
				break;
			case SHORT:
				mv.visitInsn(L2I);
				mv.visitInsn(I2S);
				break;
			case INT:
				mv.visitInsn(L2I);
				break;
			case LONG:
				// no action
				break;
			case DOUBLE:
				mv.visitInsn(L2D);
				break;
			case FLOAT:
				mv.visitInsn(L2F);
				break;
			default:
				break;
			}
			break;
		case DOUBLE:
			switch (val2.getType()) {
			case BYTE:
				mv.visitInsn(D2I);
				mv.visitInsn(I2B);
				break;
			case SHORT:
				mv.visitInsn(D2I);
				mv.visitInsn(I2S);
				break;
			case INT:
				mv.visitInsn(D2I);
				break;
			case LONG:
				mv.visitInsn(D2L);
				break;
			case DOUBLE:
				// no action
				break;
			case FLOAT:
				mv.visitInsn(D2F);
				break;
			default:
				break;
			}
			break;
		case FLOAT:
			switch (val2.getType()) {
			case BYTE:
				mv.visitInsn(F2I);
				mv.visitInsn(I2B);
				break;
			case SHORT:
				mv.visitInsn(F2I);
				mv.visitInsn(I2S);
				break;
			case INT:
				mv.visitInsn(F2I);
				break;
			case LONG:
				mv.visitInsn(F2L);
				break;
			case DOUBLE:
				mv.visitInsn(F2D);
				break;
			case FLOAT:
				// no action
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}

		if (operation.equals("+")) {
			switch (val2.getType()) {
			case BYTE:
			case SHORT:
			case INT:
				mv.visitInsn(IADD);
				break;
			case LONG:
				mv.visitInsn(LADD);
				break;
			case DOUBLE:
				mv.visitInsn(DADD);
				break;
			case FLOAT:
				mv.visitInsn(FADD);
				break;
			default:
				break;
			}
		}

		if (operation.equals("-")) {
			switch (val2.getType()) {
			case BYTE:
			case SHORT:
			case INT:
				mv.visitInsn(ISUB);
				break;
			case LONG:
				mv.visitInsn(LSUB);
				break;
			case DOUBLE:
				mv.visitInsn(DSUB);
				break;
			case FLOAT:
				mv.visitInsn(FSUB);
				break;
			default:
				break;
			}
		}

		if (operation.equals("*")) {
			switch (val2.getType()) {
			case BYTE:
			case SHORT:
			case INT:
				mv.visitInsn(IMUL);
				break;
			case LONG:
				mv.visitInsn(LMUL);
				break;
			case DOUBLE:
				mv.visitInsn(DMUL);
				break;
			case FLOAT:
				mv.visitInsn(FMUL);
				break;
			default:
				break;
			}
		}

		if (operation.equals("/")) {
			switch (val2.getType()) {
			case BYTE:
			case SHORT:
			case INT:
				mv.visitInsn(IDIV);
				break;
			case LONG:
				mv.visitInsn(LDIV);
				break;
			case DOUBLE:
				mv.visitInsn(DDIV);
				break;
			case FLOAT:
				mv.visitInsn(FDIV);
				break;
			default:
				break;
			}
		}

		if (compOp) {
			compileComparisons(mv, val1, val2, operation);
			return TypeRepresentation.BOOL;
		}

		// lastly, we need to change value from int to lower type, if it is
		// actually lower type
		switch (val2.getType()) {
		case BYTE:
			mv.visitInsn(I2B);
			break;
		case SHORT:
			mv.visitInsn(I2S);
			break;
		default:
			break;
		}

		List<TypeRepresentation> newTrList = new ArrayList<TypeRepresentation>(
				argList);
		newTrList.remove(newTrList.size() - 1);
		return doCompileMath(mv, operation, newTrList, compOp);
	}

	private void compileComparisons(MethodVisitor mv, TypeRepresentation val1,
			TypeRepresentation val2, String operation) {

		Label cTrue = new Label();
		Label cEnd = new Label();

		if (operation.equals("=")
				&& (val2.isCustomType() || val2.isComplexType())
				&& (val1.isCustomType() || val1.isComplexType())) {
			mv.visitJumpInsn(IF_ACMPEQ, cTrue);
		} else {
			switch (val2.getType()) {
			case LONG:
				mv.visitInsn(LCMP);
				mv.visitInsn(ICONST_0);
				break;
			case DOUBLE:
				mv.visitInsn(DCMPG);
				mv.visitInsn(ICONST_0);
				break;
			case FLOAT:
				mv.visitInsn(FCMPG);
				mv.visitInsn(ICONST_0);
				break;
			default:
				break;
			}

			switch (val2.getType()) {
			case BYTE:
			case SHORT:
			case INT:
			case LONG:
			case DOUBLE:
			case FLOAT:
				mv.visitJumpInsn(CompilerUtils.cmpChoice(operation), cTrue);
				break;
			default:
				throw new TypeException("incomparable types " + val2 + ", "
						+ val1);
			}
		}

		mv.visitInsn(ICONST_0);
		mv.visitJumpInsn(GOTO, cEnd);
		mv.visitLabel(cTrue);
		mv.visitInsn(ICONST_1);
		mv.visitLabel(cEnd);
	}

	/**
	 * Compiles atom, returns atom's type as type
	 * 
	 * @param mv
	 * @param atom
	 * @param last
	 * @return
	 */
	private TypeRepresentation compileAtom(MethodVisitor mv, AtomContext atom,
			boolean last) {
		Label opLabel = new Label();
		mv.visitLabel(opLabel);
		mv.visitLineNumber(atom.start.getLine(), opLabel);

		if (atom.simple_op() != null) {
			return compileSimpleOp(mv, atom.simple_op(), last);
		}

		if (atom.getText().equals("none")) {
			// null
			// returns null type
			mv.visitInsn(ACONST_NULL);
			return TypeRepresentation.NULL;
		}

		if (atom.getText().equals("true")) {
			// true
			// returns bool type
			mv.visitInsn(ICONST_1);
			return TypeRepresentation.BOOL;
		}

		if (atom.getText().equals("false")) {
			// false
			// returns bool type
			mv.visitInsn(ICONST_0);
			return TypeRepresentation.BOOL;
		}

		if (atom.cast() != null) {
			// cast operation
			// returns cast type
			TypeRepresentation tr = compileAtom(mv, atom.cast().atom(), true);
			TypeRepresentation as = CompilerUtils.asType(atom.cast().type(),
					imports);
			CompilerUtils.congruentCast(mv, tr, as);
			return as;
		}

		if (atom.accessor() != null) {
			// accessor operation
			// return type of last nested accessor
			TypeRepresentation varType = null;
			for (IdentifierContext i : atom.accessor().dottedName()
					.identifier()) {
				String name = i.getText();
				if (varType == null) {
					varType = fc.autoToType(name);
					CompilerUtils.addLoad(mv, fc.autoToNum(name), varType);
				} else {
					if (!varType.isCustomType())
						throw new TypeException(
								"Object is not typedef at line "
										+ i.start.getLine());
					FieldSignatures fs = BuildPath.getBuildPath()
							.getFieldSignatures(varType.getFqTypeName());
					TypeRepresentation nvarType = fs.getType(name);
					mv.visitFieldInsn(GETFIELD,
							varType.getJavaClassName(), name,
							nvarType.toJVMTypeString());
					varType = nvarType;
				}
			}
			return varType;
		}

		if (atom.constArg() != null) {
			// constant
			ConstArgContext c = atom.constArg();

			if (c.IntegerConstant() != null) {
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

			if (c.LongConstant() != null) {
				// long
				String text = c.getText();
				text = text.substring(0, text.length() - 1);

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

			if (c.DoubleConstant() != null) {
				// double
				String text = c.getText();
				double dv = Double.parseDouble(text);
				mv.visitLdcInsn(dv);
				return TypeRepresentation.DOUBLE;
			}

			if (c.FloatConstant() != null) {
				// float
				String text = c.getText();
				text = text.substring(0, text.length() - 1);
				float fv = Float.parseFloat(text);
				mv.visitLdcInsn(fv);
				return TypeRepresentation.FLOAT;
			}

			if (c.String() != null) {
				// string
				String strValue = c.getText();
				strValue = Utils.removeEscapes(strValue);
				strValue = strValue.substring(1, strValue.length() - 1);
				mv.visitLdcInsn(strValue);
				return TypeRepresentation.STRING;
			}

			if (c.make() != null || c.clone() != null) {
				// clone or assign
				boolean clone = c.clone() != null;
				List<AssignmentsContext> ac = clone ? c.clone().assignments()
						: c.make().assignments();
				FqNameContext typeFQName = clone ? c.clone().fqName() : c
						.make().fqName();

				String tt = typeFQName.getText();
				String fqPath = imports.importMapOriginal.get(tt);
				if (fqPath == null && tt.contains("."))
					fqPath = tt;
				if (fqPath == null)
					throw new MalformedImportDeclarationException("type " + tt
							+ " is not defined");

				TypeRepresentation tr = new TypeRepresentation();
				tr.setType(SystemTypes.CUSTOM);
				tr.setFqTypeName(fqPath);

				mv.visitTypeInsn(NEW, tr.getJavaClassName());
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, tr.getJavaClassName(),
						"<init>", "()V", false);

				FieldSignatures fs = BuildPath.getBuildPath()
						.getFieldSignatures(tr.getFqTypeName());

				if (fs == null)
					throw new MalformedImportDeclarationException("type " + tt
							+ " is not defined");

				// clone fields
				if (clone) {
					TypeRepresentation cloneType = compileAtom(mv, c.clone()
							.atom(), true);
					if (!CompilerUtils.congruentType(mv, tr, cloneType))
						throw new TypeException("wrong type at line "
								+ c.clone().start.getLine());

					for (String fname : fs.getFields()) {
						mv.visitInsn(DUP2);
						TypeRepresentation varType = fs.getType(fname);
						mv.visitFieldInsn(GETFIELD, tr.getJavaClassName(),
								fname, varType.toJVMTypeString());
						mv.visitFieldInsn(PUTFIELD, tr.getJavaClassName(),
								fname, varType.toJVMTypeString());
					}

					mv.visitInsn(POP);
				}

				// assign fields
				for (AssignmentsContext a : ac) {
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitLineNumber(a.start.getLine(), l0);

					for (AssignmentContext assign : a.assignment()) {
						mv.visitInsn(DUP);
						String fname = assign.identifier().getText();
						TypeRepresentation varType = fs.getType(fname);
						TypeRepresentation actualType = compileAtom(mv,
								assign.atom(), true);
						if (!CompilerUtils.congruentType(mv, varType,
								actualType))
							throw new TypeException("wrong type at line "
									+ assign.start.getLine());
						mv.visitFieldInsn(PUTFIELD, tr.getJavaClassName(),
								fname, varType.toJVMTypeString());
					}
				}

				return tr;
			}

			if (c.funcptr() != null) {
				// function pointer
				String fncName = c.funcptr().funcdesignator().getText();
				String fqPath = imports.importMapOriginal.get(fncName);
				if (fqPath == null && fncName.contains("."))
					fqPath = fncName;
				if (fqPath == null)
					throw new MalformedImportDeclarationException("function "
							+ fncName + " is not defined");
				String typeName = fqPath.substring(0, fqPath.lastIndexOf('.'));
				String cp = BuildPath.getBuildPath().getClassPath(typeName);

				if (cp == null)
					throw new MalformedImportDeclarationException("function "
							+ fncName + " is not defined");

				mv.visitLdcInsn(fncName);
				mv.visitLdcInsn(Type.getType(Utils.asLName(fqThisType)));
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class",
						"getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitLdcInsn(cp);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader",
						"loadClass", "(Ljava/lang/String;)Ljava/lang/Class;",
						false);
				mv.visitMethodInsn(
						INVOKESTATIC,
						"cz/upol/vanusanik/disindent/runtime/types/Method",
						"makeFunction",
						"(Ljava/lang/String;Ljava/lang/Class;)Lcz/upol/vanusanik/disindent/runtime/types/Method;",
						false);
				return TypeRepresentation.FUNCTION;
			}
		}

		if (atom.constList() != null) {
			// list type
			TypeRepresentation st = CompilerUtils.asType(atom.constList()
					.type(), imports);
			TypeRepresentation tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(st);

			if (atom.constList().atoms() != null) {
				for (AtomContext a : atom.constList().atoms().atom()) {
					TypeRepresentation atomType = compileAtom(mv, a, true);
					if (!CompilerUtils.congruentType(mv, atomType, st))
						throw new TypeException("wrong type at line "
								+ a.start.getLine());
				}

				mv.visitInsn(ACONST_NULL);

				for (int i = 0; i < atom.constList().atoms().atom().size(); i++) {
					mv.visitMethodInsn(
							INVOKESTATIC,
							"cz/upol/vanusanik/disindent/runtime/types/DList",
							"constList",
							"(Ljava/lang/Object;Lcz/upol/vanusanik/disindent/runtime/types/DList;)Lcz/upol/vanusanik/disindent/runtime/types/DList;",
							false);
				}
			}

			return tr;
		}

		return null;
	}
}
