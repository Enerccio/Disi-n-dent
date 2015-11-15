package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.upol.vanusanik.disindent.errors.MethodAlreadyExistException;

/**
 * Implements function signature node. 
 * @author Peter Vanusanik
 *
 */
public class FunctionSignature implements Serializable {
	private static final long serialVersionUID = -6656246373534259069L;

	/**Completed name of the signature, if nonnull, it is terminating node */
	private SignatureSpecifier levelName;
	/** Subsignatures, if any */
	private Map<TypeRepresentation, FunctionSignature> subsignatures
		= new HashMap<TypeRepresentation, FunctionSignature>();
	
	public static class SignatureSpecifier implements Serializable {
		private static final long serialVersionUID = 4301832532980516571L;
		public final String functionName;
		public final String javaSignature;
		public SignatureSpecifier(String functionName, String javaSignature) {
			super();
			this.functionName = functionName;
			this.javaSignature = javaSignature;
		}
	}
	
	public FunctionSignature(String name, String signSpec, List<TypeRepresentation> trList, String retSign){
		if (trList.isEmpty()){
			levelName = new SignatureSpecifier(name, signSpec+")"+retSign);
		} else {
			reparse(name, signSpec, trList, retSign);
		}
	}

	/**
	 * Adds to this signatures any subsignatures
	 * @param name
	 * @param signSpec
	 * @param trList
	 */
	void reparse(String name, String signSpec, List<TypeRepresentation> trList, String retSign) {
		TypeRepresentation tr = trList.get(0);
		List<TypeRepresentation> sublist = trList.subList(1, trList.size());
		normalType(tr, name, signSpec, trList, sublist, retSign);
	}

	/**
	 * Adds normal type to the subsignatures
	 * @param tr
	 * @param name
	 * @param signSpec
	 * @param trList
	 * @param sublist
	 */
	private void normalType(TypeRepresentation tr, String name, String signSpec,
			List<TypeRepresentation> trList, List<TypeRepresentation> sublist, String retSign) {
		if (subsignatures.containsKey(tr) && sublist.size() == 0)
			throw new MethodAlreadyExistException();
		
		if (retSign == null)
			retSign = tr.toJVMTypeString();
		else
			signSpec = signSpec + tr.toJVMTypeString();
		
		if (!subsignatures.containsKey(tr))
			subsignatures.put(tr, new FunctionSignature(name, signSpec, sublist, retSign));
		else
			subsignatures.get(tr).reparse(name, signSpec, sublist, retSign);
	}
	
	/**
	 * Searches signature tree for the fq function name
	 * @param trList
	 * @return
	 */
	public SignatureSpecifier methodName(List<TypeRepresentation> trList){
		if (trList.size() == 0)
			return levelName;
		if (subsignatures.containsKey(trList.get(0))){
			return subsignatures.get(trList.get(0)).methodName(trList.subList(1, trList.size()));
		}
		return null;
	}
}
