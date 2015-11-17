package cz.upol.vanusanik.disindent.runtime.types;

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
		cont:
		for (java.lang.reflect.Method m : clazz.getMethods()){
			if (m.getName().equals(methodName)){
				if (m.getParameterCount() != parameters.length)
					continue;
				for (int i=0; i<m.getParameterCount(); i++){
					Class<?> c1 = parameters[i];
					Class<?> c2 = m.getParameters()[i].getType();
					
					if (c1 != null && !c1.equals(c2)){
						if (c2.isPrimitive()){
							if (c1.equals(Double.class) && c2.equals(double.class))
								continue;
							if (c1.equals(Float.class) && c2.equals(float.class))
								continue;
							if (c1.equals(Integer.class) && c2.equals(int.class))
								continue;
							if (c1.equals(Long.class) && c2.equals(long.class))
								continue;
							if (c1.equals(Byte.class) && c2.equals(byte.class))
								continue;
							if (c1.equals(Short.class) && c2.equals(short.class))
								continue;
							if (c1.equals(Boolean.class) && c2.equals(boolean.class))
								continue;
						}
						continue cont;
					} if (c1 == null){
						if (c2.isPrimitive())
							continue cont;
					}
				}
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
