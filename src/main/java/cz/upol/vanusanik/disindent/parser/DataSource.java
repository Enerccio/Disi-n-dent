package cz.upol.vanusanik.disindent.parser;

import java.io.InputStream;

/**
 * Interface providing input stream of source file contents along with its original filename 
 * @author Peter Vanusanik
 *
 */
public interface DataSource {
	
	/**
	 * Returns InputStream containing din source code
	 * @return din source code in InputStream
	 */
	public InputStream getData();
	/**
	 * @return file name of the din source code, should have .din extension
	 */
	public String	   getFilename();

}
