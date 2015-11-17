import cz.upol.vanusanik.disindent.DisindentThread;
import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.utils.Utils;

public class Test {

	public static void main(String[] args) throws Exception{
		DisindentThread.simpleStart("bin/");
		
		ClassLoader cl = BuildPath.getBuildPath().getClassLoader();
		
		Class<?> newClass = cl.loadClass(Utils.asModuledefJavaName("Test"));
		Object o = newClass.newInstance();
		System.out.println(o);
		
		// precompile for correct ct utils
		cl.loadClass(Utils.asModuledefJavaName("System"));
		cl.loadClass(Utils.asModuledefJavaName("Math"));
		
		long ct = System.currentTimeMillis();
		Object o2 = o.getClass().getMethod("foo", boolean.class, double.class, double.class).invoke(null, false, 1.0, 2.0);
		System.out.println(System.currentTimeMillis() - ct + " ms");
		System.out.println(o2);
	}
}
