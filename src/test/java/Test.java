import cz.upol.vanusanik.disindent.DisindentThread;
import cz.upol.vanusanik.disindent.buildpath.BuildPath;

public class Test {

	public static void main(String[] args) throws Exception{
		DisindentThread.simpleStart("bin/");
		
		ClassLoader cl = BuildPath.getBuildPath().getClassLoader();
		
		Class<?> newClass = cl.loadClass("test");
		Object o = newClass.newInstance();
		System.out.println(o);
	}
	
}
