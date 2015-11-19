package cz.upol.vanusanik.disindent.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.compiler.DisindentCompiler;
import cz.upol.vanusanik.disindent.parser.DataSource;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;
import cz.upol.vanusanik.disindent.utils.Utils;

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
		if (Utils.disindentClass(name)) {
			if (!classCache.containsKey(name)){
				synchronized (this){
					if (!classCache.containsKey(name)){
						byte[] data = load(name);
						classCache.put(name, defineClass(name, data, 0, data.length));
					}
				}
			}
			if (!classCache.containsKey(name))
				throw new ClassNotFoundException();
			return classCache.get(name);
		} else {
			return Thread.currentThread().getContextClassLoader().loadClass(name);
		}
	}

	private byte[] load(String name) throws ClassNotFoundException {
		if (caches.containsKey(name))
			return caches.get(name);
		
		String[] contents = Utils.splitByLastSlash(name);
		String module = contents[1];
		String path = contents[0];
		
		String exactModule = module;
		if (StringUtils.countMatches(module, '$') > 3){
			int idx = StringUtils.ordinalIndexOf(module, "$", 4);
			exactModule = module.substring(0, idx);
		}
		
		BuildPath bp = BuildPath.getBuildPath();
		DataSource ds = bp.getClassSource((path.equals("") ? "" : path + "/") + exactModule);
		
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
