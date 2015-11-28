package cz.upol.vanusanik.disindent.compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.CompilerOptions;
import cz.upol.vanusanik.disindent.buildpath.FieldSignatures;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.errors.BuildPathException;
import cz.upol.vanusanik.disindent.errors.CompilationException;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;
import cz.upol.vanusanik.disindent.utils.Utils;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqtypeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Toplevel_formContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Type_bodyContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Type_declContext;

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
	private cz.upol.vanusanik.disindent.compiler.ScopeContext sc;
	/** fully qualified java type name */
	private String fqThisType;
	/** used for creating tempvars */
	private long gensymCount = 0;
	/** class validation list */
	private List<byte[]> validateList = new ArrayList<byte[]>();

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
		// setup compilation
		CompilerOptions co = new CompilerOptions(BuildPath.getBuildPath().getGlobalOptions());
		
		co.set("__filename__", filename, true);
		co.set("__module_name__", moduleName, true);
		
		ProgramContext pc = parser.program();
		
		if (pc.package_decl().size() > 1){
			throw new BuildPathException("multiple package declarations in one file");
		}
		if (pc.native_decl().size() > 1){
			throw new BuildPathException("multiple native package declarations in one file");
		}
		
		if (pc.package_decl().size() == 1)
			packageName = pc.package_decl(0).complex_identifier().getText().replace("::", ".");
		if (pc.native_decl().size() == 1){
			nativePackage = pc.native_decl(0).complex_identifier().getText().replace("::", ".");
		}
		
		String slashPath = Utils.slashify(packageName);
		
		fqThisType = slashPath.equals("") ? Utils.asModuledefJavaName(moduleName) : (slashPath  + "/" + Utils.asModuledefJavaName(moduleName));
		
		co.set("__package_name__", packageName, true);
		co.set("__native_package__", nativePackage, true);
		co.set("__java_name__", fqThisType, true);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
				| ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, fqThisType, null,
				"java/lang/Object", new String[]{"java/io/Serializable"});
		cw.visitSource(filename, null);
		
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		Label start = new Label();
		mv.visitLabel(start);
		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		
		String contextPath = fqThisType;
		TypeRepresentation contextType = new TypeRepresentation();
		contextType.setType(SystemTypes.MODULE);
		contextType.setFqTypeName(contextPath);
		
		Map<String, String> imports = Imports.parseImports(pc, moduleName, packageName);
		Map<String, String> aliasMap = new HashMap<String, String>();
		
		sc = new ScopeContext();
		sc.pushNewFunc(null, contextType);
		sc.push();
		
		sc.addLocal("$this", contextType);
		compileImports(cw, mv, imports, aliasMap);
		
		for (Toplevel_formContext topLevel : pc.toplevel_form())
			compileToplevel(topLevel, cw, mv, imports, aliasMap);
		
		mv.visitInsn(RETURN);		
		sc.pop(mv, start, true);
		sc.popFunctionBlock();
		
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		cw.visitEnd();
		
		byte[] classData = cw.toByteArray();
		validateList.add(classData);
		
		if (co.isDefined("__dev_debug")){
			for (byte[] cd : validateList){
				PrintWriter pw = new PrintWriter(System.out);
				try {
					CheckClassAdapter.verify(new ClassReader(cd), cl, true, pw);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		
		cl.addClass(Utils.deslashify(fqThisType), classData);
	}

	private void compileToplevel(Toplevel_formContext topLevel, ClassWriter cw,
			MethodVisitor mv, Map<String, String> imports,
			Map<String, String> aliasMap) {
		
		Label l = new Label();
		mv.visitLabel(l);
		sc.push();
		
		if (topLevel.type_decl() != null){
			compileType(topLevel.type_decl(), cw, mv, imports, aliasMap);
		}
		
		if (topLevel.native_type() != null){
			// TODO
		}
		
		if (topLevel.func_decl() != null){
			// TODO
		}
		
		if (topLevel.funn_decl() != null){
			// TODO
		}
		
		if (topLevel.define_compiler_constant() != null){
			// TODO			
		}
		
		if (topLevel.include_compiler_constants() != null){
			// TODO
		}
			
		
		sc.pop(mv, l, true);
	}

	private void compileType(Type_declContext tdc, ClassWriter ocw,
			MethodVisitor omv, Map<String, String> imports,
			Map<String, String> aliasMap) {
		
		String typedefName = tdc.identifier().getText();
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
		
		mv.visitInsn(RETURN);
		Label end = new Label();
        mv.visitLabel(end);
        mv.visitLocalVariable("$this", "L"+fqJavaPath+";", null, l0, end, 0);
        mv.visitMaxs(0, 0);
        
        for (Type_bodyContext body : tdc.type_body()){
        	FqtypeContext fq = body.fqtype();
        	
        	String name = fq.identifier().getText();
        	TypeContext tc = fq.type();
        	
        	TypeRepresentation type = CompilerUtils.asType(tc, imports);
        	cw.visitField(ACC_PUBLIC, name, type.toJVMTypeString(), null, null).visitEnd();
        }
        
        byte[] classData = cw.toByteArray();
		validateList.add(classData);
		
		cl.addClass(Utils.deslashify(fqJavaPath), classData);
	}

	private void compileImports(ClassVisitor cv, MethodVisitor mv, Map<String, String> imports,
			Map<String, String> aliasMap) {
		
		for (String key : imports.keySet()){
			String importedValue = imports.get(key);
			
			if (!BuildPath.getBuildPath().isTypedef(importedValue) && BuildPath.getBuildPath().getClassPath(importedValue) != null){
				asModule(cv, mv, key, aliasMap, importedValue);
			} else if (!BuildPath.getBuildPath().isTypedef(importedValue)){
				String[] split = Utils.splitByLastDot(importedValue);
				String module = split[0];
				String name = split[1];
				
				if (!aliasMap.containsKey(module)){
					if (!BuildPath.getBuildPath().isTypedef(module) && BuildPath.getBuildPath().getClassPath(module) != null){
						asModule(cv, mv, Utils.splitByLastDot(module)[1], aliasMap, module);
					} else
						throw new CompilationException("type " + module + " is not a module");
				}
				
				FieldSignatures fs = BuildPath.getBuildPath().getFieldSignatures(module);
			    TypeRepresentation type = fs.getType(name);
			    
			    if (type == null)
			    	throw new CompilationException("type " + module + " has no field " + name);
			    
			    sc.addLocal(name, type);
			    
			    String mtype = BuildPath.getBuildPath().getClassPath(module);
				String ltype = "L" + mtype + ";";	    
			    
			    mv.visitVarInsn(ALOAD, 0);
			    mv.visitFieldInsn(GETFIELD, fqThisType, aliasMap.get(module), ltype);
			    mv.visitFieldInsn(GETFIELD, mtype, "$"+name, type.toJVMTypeString());
			    CompilerUtils.addStore(mv, sc.autoToNum(name), type);
			}
		}
		
	}

	private void asModule(ClassVisitor cv, MethodVisitor mv, String key,
			Map<String, String> aliasMap, String importedValue) {
		String genName = "$module" + Utils.camelify(key);
		aliasMap.put(importedValue, genName);
		String type = BuildPath.getBuildPath().getClassPath(importedValue);
		String ltype = "L" + type + ";";
		
		cv.visitField(ACC_PRIVATE, genName, ltype, null, null).visitEnd();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitTypeInsn(NEW, type);
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, type, "<init>", "()V", false);
		mv.visitFieldInsn(PUTFIELD, fqThisType, genName, ltype);
	}
}
