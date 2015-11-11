package cz.upol.vanusanik.disindent.compiler;

import org.apache.commons.io.FilenameUtils;

import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;

public class DisindentCompiler {
	
	private disindentParser parser;
	private String filename;
	private String moduleName;
	
	public DisindentCompiler(String filename, disindentParser parser){
		this.parser = parser;
		this.filename = filename;
		this.moduleName = FilenameUtils.getBaseName(filename);
	}

	public Class<?> compile(){
		
		
		return null;
	}
}
