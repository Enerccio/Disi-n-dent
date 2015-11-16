package cz.upol.vanusanik.disindent.runtime;

import java.util.HashMap;
import java.util.Map;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.compiler.DisindentCompiler;
import cz.upol.vanusanik.disindent.parser.DataSource;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;

/**
 * DisindentClassLoader contains all disindent classes and allows loading
 * classes from memory.
 * 
 * @author Peter Vanusanik
 *
 */
public class DisindentClassLoader extends ClassLoader {

	private Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();
	private Map<String, byte[]> caches = new HashMap<String, byte[]>();
	
	public DisindentClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			if (!classCache.containsKey(name)){
				synchronized (this){
					if (!classCache.containsKey(name)){
						byte[] data = load(name);
						classCache.put(name, defineClass(name, data, 0, data.length));
					}
				}
			}
			if (!classCache.containsKey(name))
				return Thread.currentThread().getContextClassLoader().loadClass(name);
			return classCache.get(name);
		} catch (Throwable e){
			try {
				return Thread.currentThread().getContextClassLoader().loadClass(name);
			} catch (Throwable e1){
				throw e;
			}
		}
	}

	private byte[] load(String name) throws ClassNotFoundException {
		if (caches.containsKey(name))
			return caches.get(name);
		
		BuildPath bp = BuildPath.getBuildPath();
		DataSource ds = bp.getClassSource(name);
		
		if (ds == null) // class not found
			throw new ClassNotFoundException("Class '" + name + "' not found ");
		
		ParserBuilder bd = new ParserBuilder();
		bd.setDataSource(ds);
		new DisindentCompiler(ds.getFilename(), bd.build(), this).compile();
		
		return caches.get(name);
	}
	
	public synchronized void addClass(String slashFQName, byte[] data){
		caches.put(slashFQName, data);
	}
}
