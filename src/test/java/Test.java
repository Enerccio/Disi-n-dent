import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;

public class Test {

	public static void main(String[] args) throws Exception{
		
		ClassLoader cl = new DisindentClassLoader(Test.class.getClassLoader());
		
		Class<?> newClass = cl.loadClass("test");
		Object o = newClass.newInstance();
		System.out.println(o);
	}
	
}
