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
	}
	
}
