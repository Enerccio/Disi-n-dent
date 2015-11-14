package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class FunctionSignatures implements Serializable {
	private static final long serialVersionUID = -7185025760839548335L;

	private Map<String, FunctionSignature> functions = new HashMap<String, FunctionSignature>();
	private Set<String> functionNames = new HashSet<String>();
	
	public void addFunction(String baseName, List<TypeRepresentation> types){
		if (!functions.containsKey(baseName)){
			functions.put(baseName, new FunctionSignature(baseName, null, types));
		} else {
			functions.get(baseName).reparse(baseName, null, types);
		}
		functionNames.add(baseName);
	}
	
	public boolean hasFunctionWithName(String name){
		return functionNames.contains(name);
	}
}
