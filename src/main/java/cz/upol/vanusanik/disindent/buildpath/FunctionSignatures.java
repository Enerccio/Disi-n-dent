package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.upol.vanusanik.disindent.buildpath.FunctionSignature.SignatureSpecifier;

public class FunctionSignatures implements Serializable {
	private static final long serialVersionUID = -7185025760839548335L;

	private Map<String, FunctionSignature> functions = new HashMap<String, FunctionSignature>();
	private Set<String> functionNames = new HashSet<String>();
	
	public void addFunction(String baseName, List<TypeRepresentation> types){
		if (!functions.containsKey(baseName)){
			functions.put(baseName, new FunctionSignature(baseName, "(", types, null));
		} else {
			functions.get(baseName).reparse(baseName, "(", types, null);
		}
		functionNames.add(baseName);
	}
	
	/**
	 * Returns true if such function exists
	 * @param name
	 * @return
	 */
	public boolean hasFunctionWithName(String name){
		return functionNames.contains(name);
	}
	
	/**
	 * Returns function signature (name, signature) for name and type specifier
	 * @param name
	 * @param trl
	 * @return
	 */
	public SignatureSpecifier getSpecifier(String name, List<TypeRepresentation> trl){
		return functions.get(name).methodName(trl);
	}
}
