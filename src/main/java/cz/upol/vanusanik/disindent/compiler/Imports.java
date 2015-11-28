package cz.upol.vanusanik.disindent.compiler;

import java.util.HashMap;
import java.util.Map;

import cz.upol.vanusanik.disindent.utils.Utils;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.IdentifierContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Include_declContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Native_typeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Type_declContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Use_declContext;

/**
 * Currently used imports. Used during compilation to resolve 
 * @author Peter Vanusanik
 *
 */
public class Imports {
	
	public static Map<String, String> parseImports(ProgramContext pc,
			String selfModule, String selfPackage) {
		Map<String, String> iMap = new HashMap<String, String>();

		for (Use_declContext ud : pc.use_decl()) {
			String usingIdentifier = ud.complex_identifier().getText().replace("::", ".");
			String[] split = Utils.splitByLastDot(usingIdentifier);
			iMap.put(split[1], usingIdentifier);
		}
		
		for (Include_declContext ud : pc.include_decl()){
			String fp = ud.complex_identifier().getText().replace("::", ".");
			for (IdentifierContext ic : ud.include_list().identifier()){
				String identifier = ic.getText();
				iMap.put(identifier, fp + "." + identifier);
			}
		}
		
		for (Type_declContext tc : Utils.searchForElementOfType(Type_declContext.class, pc)){
			String typedefName = tc.identifier().getText();
			String fqName = (selfPackage.equals("") ? selfModule : selfPackage
					+ "." + selfModule)
					+ "." + typedefName;
			iMap.put(typedefName, fqName);
		}
		
		for (Native_typeContext ntc : Utils.searchForElementOfType(Native_typeContext.class, pc)){
			String typedefName = ntc.identifier().getText();
			String fqName = (selfPackage.equals("") ? selfModule : selfPackage
					+ "." + selfModule)
					+ "." + typedefName;
			iMap.put(typedefName, fqName);
		}
		
		return iMap;
	}
	
}
