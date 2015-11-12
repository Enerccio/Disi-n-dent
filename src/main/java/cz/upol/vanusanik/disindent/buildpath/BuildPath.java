package cz.upol.vanusanik.disindent.buildpath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import cz.upol.vanusanik.disindent.parser.DataSource;
import cz.upol.vanusanik.disindent.parser.SimpleDataSource;

/**
 * Represents all modules as interfaces for lookup of methods/objects in disindent.
 * @author Peter Vanusanik
 *
 */
public class BuildPath {
	private static BuildPath instance = new BuildPath();
	
	public static BuildPath getBuildPath(){
		return instance;
	}
	
	private BuildPath(){
		
	}
	
	private Map<String, AvailableElement> availableElements
		= new TreeMap<String, AvailableElement>();
	
	public void addPath(File path){
		
	}

	public DataSource getClassSource(String name) {
		AvailableElement ae = availableElements.get(name);
		if (ae == null)
			return null;
		
		if (ae.module != null)
			ae = ae.module;
		
		SimpleDataSource ds = new SimpleDataSource();
		ds.filename = ae.sourceName;
		ds.is = new ByteArrayInputStream(ae.source);
		
		return ds;
	}

}
