package cz.upol.vanusanik.disindent.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import cz.upol.vanusanik.disindent.errors.DisindentRuntimeFailure;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentLexer;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;

/**
 * Creates parser instance with provided inputs
 * @author Peter Vanusanik
 *
 */
public class ParserBuilder {
	/**
	 * Simple data source holding filename and input stream
	 * @author Enerccio
	 *
	 */
	private static class SimpleDataSource implements DataSource { 

		private InputStream is;
		private String filename;
		
		@Override
		public InputStream getData() {
			return is;
		}

		@Override
		public String getFilename() {
			return filename;
		}
		
	}
	
	private SimpleDataSource inputSource;
	
	public ParserBuilder(){
		
	}

	public ParserBuilder setDataSource(DataSource inputSource){
		SimpleDataSource ds = getDS();
		ds.is = inputSource.getData();
		ds.filename = inputSource.getFilename();
		return this;
	}
	
	public ParserBuilder setFilename(String name){
		getDS().filename = name;
		return this;
	}
	
	public ParserBuilder setInput(InputStream is){
		getDS().is = is;
		return this;
	}
	
	public ParserBuilder setInput(String input, String encoding) throws UnsupportedEncodingException{
		return setInput(input.getBytes(encoding));
	}
	
	public ParserBuilder setInput(String input) throws UnsupportedEncodingException{
		return setInput(input, "utf-8");
	}
	
	public ParserBuilder setInput(byte[] input){
		return setInput(new ByteArrayInputStream(input));
	}
	
	public ParserBuilder setInputFile(File f) throws FileNotFoundException{
		setFilename(f.getName());
		return setInput(new BufferedInputStream(new FileInputStream(f)));
	}

	private SimpleDataSource getDS() {
		if (inputSource == null)
			inputSource = new SimpleDataSource();
		return inputSource;
	}
	
	public disindentParser build(){
		try {
			ANTLRInputStream is = new ANTLRInputStream(inputSource.is);
			disindentLexer lexer = new disindentLexer(is);
			lexer.removeErrorListeners();
			lexer.addErrorListener(new ThrowingErrorListener(is.name));
			CommonTokenStream stream = new CommonTokenStream(lexer);
			disindentParser parser = new disindentParser(stream);

			parser.removeErrorListeners();
			parser.addErrorListener(new ThrowingErrorListener(is.name));
			return parser;
		} catch (IOException e) {
			throw new DisindentRuntimeFailure(e);
		} finally {
			inputSource = null;
		}
	}
}
