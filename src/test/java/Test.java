import cz.upol.vanusanik.disindent.compiler.DisindentCompiler;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;


public class Test {

	public static void main(String[] args) throws Exception{
		
		ParserBuilder bd = new ParserBuilder();
		bd.setFilename("test.din");
		bd.setInput(Test.class.getResourceAsStream("test.din"));
		DisindentCompiler c = new DisindentCompiler("test.din", bd.build());
		
		
	}
	
}
