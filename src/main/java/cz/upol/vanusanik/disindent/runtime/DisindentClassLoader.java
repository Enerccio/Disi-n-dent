package cz.upol.vanusanik.disindent.runtime;

import java.util.HashMap;
import java.util.Map;

import cz.upol.vanusanik.disindent.utils.Utils;

/**
 * DisindentClassLoader contains all disindent classes and allows loading
 * classes from memory.
 * 
 * @author Peter Vanusanik
 *
 */
public class DisindentClassLoader extends ClassLoader {

	private Map<String, byte[]> assignedClasses = new HashMap<String, byte[]>();
	private Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

	public DisindentClassLoader(ClassLoader parent) {
		super(parent);
	}

	public Class<?> findClass(String name) {
		if (!classCache.containsKey(name)){
			synchronized (this){
				if (!classCache.containsKey(name)){
					byte[] data = load(name);
					classCache.put(name, defineClass(name, data, 0, data.length));
				}
			}
		}
		return classCache.get(name);
	}

	private byte[] load(String name) {
		return assignedClasses.get(name);
	}

	public void addClass(byte[] classData, String className, String packageName) {
		String fullname = Utils.fullNameForClass(className, packageName);
		assignedClasses.put(fullname, classData);
	}
}
