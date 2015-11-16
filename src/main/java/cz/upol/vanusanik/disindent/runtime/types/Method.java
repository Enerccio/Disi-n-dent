package cz.upol.vanusanik.disindent.runtime.types;

import java.util.Arrays;

/**
 * Wraps java.lang.reflect.Method into custom Method class
 * @author Peter Vanusanik
 *
 */
public class Method {
	
	/** method name */
	private String methodName;
	/** bound class */
	private Class<?> clazz;
	
	/**
	 * Returns method for specified parameters
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 */
	public java.lang.reflect.Method getMethod(Class<?>... parameters) throws NoSuchMethodException{
		for (java.lang.reflect.Method m : clazz.getMethods()){
			if (m.getName().equals(methodName)){
				if (Arrays.equals(m.getParameters(), parameters))
					return m;
			}
		}
		throw new NoSuchMethodException();
	}

	/**
	 * Creates new instance of Method object with specified name and bound clazz
	 * @param name
	 * @param clazz
	 * @return
	 */
	public static Method makeFunction(String name, Class<?> clazz){
		Method m = new Method();
		m.methodName = name;
		m.clazz = clazz;
		return m;
	}
}
