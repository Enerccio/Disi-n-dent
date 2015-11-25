package cz.upol.vanusanik.disindent.runtime.types;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps java.lang.reflect.Method into custom Method class
 * @author Peter Vanusanik
 *
 */
public abstract class Method implements Serializable {
	private static final long serialVersionUID = 5071105035981928495L;
	
	private transient Map<List<Class<?>>, java.lang.reflect.Method> handles
		= new HashMap<List<Class<?>>, java.lang.reflect.Method>();
	
	public Object invoke(Object... parameters) throws Throwable {
		
		Class<?>[] classes = new Class<?>[parameters.length];
    	for (int i=0; i<parameters.length; i++)
    		classes[i] = parameters[i] == null ? null : parameters[i].getClass();
    	List<Class<?>> cl = Arrays.asList(classes);
    	if (!handles.containsKey(cl))
	    	synchronized (handles){
	    		if (!handles.containsKey(cl))
	    			handles.put(cl, getMethod(classes));
	    	}
    	return handles.get(cl).invoke(null, parameters);
	}

	/**
	 * Returns method for specified parameters
	 * @param parameters
	 * @return
	 * @throws NoSuchMethodException
	 */
	private java.lang.reflect.Method getMethod(Class<?>... parameters) throws NoSuchMethodException {
		cont:
		for (java.lang.reflect.Method m : getClass().getMethods()){
			if (m.getName().equals("invoke")){
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
				m.setAccessible(true);
				return m;
			}
		}
		throw new NoSuchMethodException();
	}

}
