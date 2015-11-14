package cz.upol.vanusanik.disindent.compiler;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import cz.upol.vanusanik.disindent.errors.CompilationException;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;
import cz.upol.vanusanik.disindent.utils.Utils;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqModuleNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FunctionContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.IdentifierContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypedefContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.UsesContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_functionsContext;

/**
 * DisindentCompiler compiles disindent (din) files into java classes
 * @author Peter Vanusanik
 *
 */
public class DisindentCompiler implements Opcodes {
	
	/** parser instance */
	private disindentParser parser;
	/** filename of the input string, module name is created from it*/
	private String filename;
	/** name of the module being compiled, each din file is one module */
	private String moduleName;
	/** bound class loader */
	private DisindentClassLoader cl;
	/** imports are listed here */
	private Imports imports = new Imports();
	
	public DisindentCompiler(String filename, disindentParser parser, DisindentClassLoader loader){
		this.parser = parser;
		this.filename = filename;
		this.moduleName = FilenameUtils.getBaseName(filename);
		this.cl = loader;
	}
	
	/** Package name of this module */
	private String packageName = "";

	/**
	 * Commence the compilation of the loaded module. Will return classes created in this module via CompilationResult
	 * @return all java classes compiled from this module
	 * @throws CompilationException if anything goes wrong
	 */
	public void compile() throws CompilationException {
		try {
			compileModule();
		} catch (CompilationException e){
			throw e;
		} catch (Throwable t){
			throw new CompilationException(t);
		}
	}
	
	/**
	 * Compiles module in this compiler and adds it into compilation result
	 */
	private void compileModule() {
		byte[] moduleByteData = doCompileModule();
		
		cl.addClass(Utils.slashify(Utils.fullNameForClass(Utils.asModuledefJavaName(moduleName), packageName)), 
				moduleByteData);
	}

	/**
	 * Compiles module into java class as byte array
	 * @return
	 */
	private byte[] doCompileModule() {
		
		ProgramContext pc = parser.program();
		if (pc.package_declaration() != null){
			packageName = pc.package_declaration().fqName().getText();
		}
		
		if (pc.using_declaration().size() != 0){
			resolveImports(pc);
		}
		
		for (TypedefContext tdc : pc.typedef()){
			compileNewType(tdc);
		}
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, Utils.asModuledefJavaName(moduleName), null, "java/lang/Object", null);
        cw.visitSource(filename, null);
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(0, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", Utils.asLName(moduleName), null, l0, l1, 0);      
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        for (FunctionContext fc : pc.function()){
        	compileFunction(cw, fc);
        }
		
		cw.visitEnd();
		
		byte[] classData = cw.toByteArray();
		
		PrintWriter pw = new PrintWriter(System.out);
	    CheckClassAdapter.verify(new ClassReader(classData), true, pw);
	    
		return classData;
	}

	/**
	 * Resolves the identifier access to other functions/modules
	 * @param pd
	 */
	private void resolveImports(ProgramContext pc) {
		for (Using_declarationContext ud : pc.using_declaration()){
			if (ud.using_module() != null){
				String fqName = ud.using_module().fqNameImport().getText();
				imports.addImport(fqName);
			} else {
				Using_functionsContext fc = ud.using_functions();
				List<UsesContext> usesList = Utils.searchForElementOfType(UsesContext.class, fc);
				FqModuleNameContext fmnc = Utils.searchForElementOfType(FqModuleNameContext.class, fc).iterator().next();
				
				String moduleName = fmnc.getText();
				for (UsesContext usc : usesList){
					for (IdentifierContext i : usc.identifier()){
						imports.addImport(moduleName, i.getText());
					}
				}
			}
		}
		
		for (TypedefContext tc : pc.typedef()){
			String typedefName = tc.typedef_header().identifier().getText();
			String fqName = (packageName.equals("") ? moduleName : packageName + "." + moduleName) + "." + typedefName;
			imports.addImport(fqName, typedefName);
 		}
	}

	/**
	 * Compiles new type
	 * @param tdc
	 */
	private void compileNewType(TypedefContext tdc) {
		
	}
	
	/**
	 * Compiles new function to this module
	 * @param cw
	 * @param fc
	 */
	private void compileFunction(ClassWriter cw, FunctionContext fc) {
		
	}
}
