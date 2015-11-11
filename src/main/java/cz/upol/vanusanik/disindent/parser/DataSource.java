package cz.upol.vanusanik.disindent.parser;

import java.io.InputStream;

/**
 * Interface providing input stream of source file contents along with its original filename 
 * @author Peter Vanusanik
 *
 */
public interface DataSource {
	
	public InputStream getData();
	public String	   getFilename();

}
