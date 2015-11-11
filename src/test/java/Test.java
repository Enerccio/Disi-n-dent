import cz.upol.vanusanik.disindent.compiler.CompilationResult;
import cz.upol.vanusanik.disindent.compiler.DisindentCompiler;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;


public class Test {

	public static void main(String[] args) throws Exception{
		
		ParserBuilder bd = new ParserBuilder();
		bd.setFilename("test.din");
		bd.setInput(Test.class.getResourceAsStream("test.din"));
		DisindentCompiler c = new DisindentCompiler("test.din", bd.build(), new DisindentClassLoader(Test.class.getClassLoader()));
		
		CompilationResult cr = c.compile();
		Class<?> newClass = cr.getClass("test");
		Object o = newClass.newInstance();
		System.out.println(o);
	}
	
}
