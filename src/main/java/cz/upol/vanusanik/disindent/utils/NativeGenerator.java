package cz.upol.vanusanik.disindent.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ListContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.NativeImportContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ParameterContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypepartContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;

/**
 * Generates .java files from din files containing native declarations
 * @author Peter Vanusanik
 *
 */
public class NativeGenerator {
	
	public static void main(String[] args) throws Exception {
		File searchDir = new File(args[0]);
		File exportDir = new File(args[1]);
		new NativeGenerator().find(searchDir, exportDir);
	}

	/**
	 * Searches the path for the din source files
	 * @param path search path for din files
	 * @param exportDir where to store .java files
	 */
	private void find(File path, File exportDir) throws Exception {
		File[] subelements = path.listFiles();
		for (File subelement : subelements){
			if (subelement.isDirectory())
				find(subelement, exportDir);
			else {
				String name = subelement.getName();
				String ext = FilenameUtils.getExtension(name);
				
				if (ext.equals("din")){
					generateNative(subelement, name, exportDir);
				}
			}
		}
	}

	/**
	 * Generates native file for din file, it if has native elements
	 * @param path
	 * @param name
	 * @param exportDir
	 * @throws Exception
	 */
	private void generateNative(File path, String name, File exportDir) throws Exception {
		name = FilenameUtils.getBaseName(StringUtils.capitalize(StringUtils.lowerCase(name)));
		ParserBuilder pb = new ParserBuilder();
		byte[] source = FileUtils.readFileToByteArray(path);
		pb.setInput(source);
		pb.setFilename(path.getName());
		disindentParser p = pb.build();
		generateNative(p.program(), name, exportDir);
	}

	private void generateNative(ProgramContext pc, String name, File exportDir) throws Exception {
		if (pc.nativeImport().size() == 0)
			return; // no natives in this din file
		
		String nativePackage = "";
		
		if (pc.native_declaration() != null)
			nativePackage = pc.native_declaration().javaName().getText();
		
		StringBuilder bd = new StringBuilder();
		bd.append("// autogenerated java class stub\n");
		if (!nativePackage.equals(""))
			bd.append(String.format("package %s;\n", nativePackage));
		
		bd.append("\n");
		bd.append("import cz.upol.vanusanik.disindent.runtime.types.Method;\n");
		bd.append("import cz.upol.vanusanik.disindent.runtime.types.DList;\n");
		bd.append("\n");

		bd.append(String.format("public class %s {\n", name));
		
		for (NativeImportContext nic : pc.nativeImport()){
			String retType = asJavaType(nic.type());
			String funcName = nic.identifier().getText();
			List<String> args = new ArrayList<String>();
			
			if (nic.func_arguments().parameters() != null){
				for (ParameterContext p : nic.func_arguments().parameters().parameter()){
					String pname = p.identifier().getText();
					String ptype = asJavaType(p.type());
					args.add(ptype + " " + pname);
				}
			}
			
			bd.append(String.format("    public static %s %s(%s){\n        // TODO\n    }\n\n", retType, funcName, StringUtils.join(args, ", ")));
		}
		
		bd.append("}\n");
		
		String clazz = bd.toString();
		
		exportDir = exportDir(nativePackage, exportDir);
		exportDir.mkdirs();
		
		File exportFile = new File(exportDir, name+".java");
		if (exportFile.exists() || exportFile.length() != 0)
			return; // ignore existing file to not overwrite
		FileUtils.write(exportFile, clazz);
	}

	/**
	 * Creats export dir from package and export dir
	 * @param nativePackage
	 * @param exportDir
	 * @return
	 */
	private File exportDir(String nativePackage, File exportDir) {
		if (nativePackage.equals(""))
			return exportDir;
		String[] split = StringUtils.split(nativePackage, ".");
		for (String s : split){
			exportDir = new File(exportDir, s);
		}
		return exportDir;
	}

	/**
	 * Returns type context as java type
	 * @param type
	 * @return
	 */
	private String asJavaType(TypeContext type) {
		TypepartContext typepc = type.typepart();
		
		String tt;
		if (typepc.fqName() != null)
			tt = typepc.fqName().getText();
		else
			tt = typepc.getText();
		
		TypeRepresentation tr = null;
		
		tr = TypeRepresentation.asSimpleType(tt);
		if (tr == null){
			String fqPath = "---";
			
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.CUSTOM);
			tr.setFqTypeName(fqPath);
		}
		
		for (@SuppressWarnings("unused") ListContext lc : type.list()){
			TypeRepresentation oldr = tr;
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(oldr);
		}
		
		return tr.toJaveTypeString();
	}

}
