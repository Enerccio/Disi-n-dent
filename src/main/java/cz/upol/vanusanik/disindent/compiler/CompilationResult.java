package cz.upol.vanusanik.disindent.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the result of compilation of module. 
 * @author Peter Vanusanik
 *
 */
public class CompilationResult {

	String packageName;
	Map<String, Class<?>> compiledClasses = new LinkedHashMap<String, Class<?>>();
	
	/**
	 * @return List of class names of new classes created
	 */
	public List<String> getCompiledClasses(){
		return new ArrayList<String>(compiledClasses.keySet());
	}
	
	/**
	 * @return package name of the new classes
	 */
	public String getPackageName(){
		return packageName;
	}
	
	/**
	 * @return all newly created classes
	 */
	public Collection<Class<?>> getClasses(){
		return compiledClasses.values();
	}
	
	/**
	 * @param name
	 * @return newly create class of name name
	 */
	public Class<?> getClass(String name){
		return compiledClasses.get(name);
	}
}
