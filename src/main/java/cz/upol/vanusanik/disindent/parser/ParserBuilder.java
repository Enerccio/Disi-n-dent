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
	 * Currently built data source
	 */
	private SimpleDataSource inputSource;
	
	/**
	 * Creates new, uninitialized ParserBuilder
	 */
	public ParserBuilder(){
		
	}

	/**
	 * Set this data source's content for this parser
	 * @param inputSource data source content
	 * @return this instance for chaining
	 */
	public ParserBuilder setDataSource(DataSource inputSource){
		SimpleDataSource ds = getDS();
		ds.is = inputSource.getData();
		ds.filename = inputSource.getFilename();
		return this;
	}
	
	/**
	 * Sets the filename of this module to this name
	 * @param name new module file name
	 * @return this instance for chaining
	 */
	public ParserBuilder setFilename(String name){
		getDS().filename = name;
		return this;
	}
	
	/**
	 * Sets the source of this module to this stream
	 * @param new source of this module
	 * @return this instance for chaining
	 */
	public ParserBuilder setInput(InputStream is){
		getDS().is = is;
		return this;
	}
	
	/**
	 * Sets the source of this module to contents of this string
	 * @param new source of this module
	 * @param encoding source encoding
	 * @return this instance for chaining
	 * @throws UnsupportedEncodingException if encoding is not supported
	 */
	public ParserBuilder setInput(String input, String encoding) throws UnsupportedEncodingException{
		return setInput(input.getBytes(encoding));
	}
	
	/**
	 * Sets the source of this module to contents of this string
	 * @param new source of this module
	 * @return this instance for chaining
	 * @throws UnsupportedEncodingException if this platform does not support utf-8
	 */
	public ParserBuilder setInput(String input) throws UnsupportedEncodingException{
		return setInput(input, "utf-8");
	}
	
	/**
	 * Sets the source of this module to this InputStream (will buffer it).
	 * @param new source of this module
	 * @return this instance for chaining
	 */
	public ParserBuilder setInput(byte[] input){
		return setInput(new ByteArrayInputStream(input));
	}
	
	/**
	 * Sets the source of this module and filename based on the provided File object
	 * @param f source file
	 * @return this instance for chaining
	 * @throws FileNotFoundException
	 */
	public ParserBuilder setInputFile(File f) throws FileNotFoundException{
		setFilename(f.getName());
		return setInput(new BufferedInputStream(new FileInputStream(f)));
	}

	/**
	 * @return returns inputSource if existing, otherwise creates one
	 */
	private SimpleDataSource getDS() {
		if (inputSource == null)
			inputSource = new SimpleDataSource();
		return inputSource;
	}
	
	/**
	 * Builds new parser instance
	 * @return new parser instance
	 */
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
