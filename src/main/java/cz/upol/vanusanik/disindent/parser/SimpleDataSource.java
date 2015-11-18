package cz.upol.vanusanik.disindent.parser;

import java.io.InputStream;

/**
 * Simple data source holding filename and input stream
 * @author Peter Vanusanik
 *
 */
public class SimpleDataSource implements DataSource { 

	/** data input stream */
	public InputStream is;
	/** source filename */
	public String filename;
	
	@Override
	public InputStream getData() {
		return is;
	}

	@Override
	public String getFilename() {
		return filename;
	}
	
}