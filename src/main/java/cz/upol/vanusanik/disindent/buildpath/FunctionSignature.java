package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.errors.CompilationException;
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
		public final List<TypeRepresentation> specList;
		public final List<TypeRepresentation> parameters;
		public final TypeRepresentation retType;
		public final TypeRepresentation selfType;
		
		public SignatureSpecifier(String functionName, String javaSignature, List<TypeRepresentation> specList) {
			super();
			this.functionName = functionName;
			this.javaSignature = javaSignature;
			this.specList = Collections.unmodifiableList(new ArrayList<TypeRepresentation>(specList));
			this.parameters = Collections.unmodifiableList(new ArrayList<TypeRepresentation>(specList.subList(1, specList.size())));
			this.retType = specList.get(0);
			this.selfType = new TypeRepresentation();
			this.selfType.setType(SystemTypes.FUNCTION);
			
			specList = new ArrayList<TypeRepresentation>(specList);
			specList.remove(1); // remove context specifier because it is irrelevant in invoking
			for (TypeRepresentation tr : specList)
				this.selfType.addGenerics(tr);
			
			BuildPath.getBuildPath().registerType(this.selfType);
		}
	}
	
	public FunctionSignature(String name, String signSpec, List<TypeRepresentation> trList, String retSign, List<TypeRepresentation> oList){
		if (trList.isEmpty()){
			levelName = new SignatureSpecifier(name, signSpec+")"+retSign, oList);
		} else {
			reparse(name, signSpec, trList, retSign, oList);
		}
	}

	/**
	 * Adds to this signatures any subsignatures
	 * @param name
	 * @param signSpec
	 * @param trList
	 */
	void reparse(String name, String signSpec, List<TypeRepresentation> trList, String retSign, List<TypeRepresentation> oList) {
		TypeRepresentation tr = trList.get(0);
		List<TypeRepresentation> sublist = trList.subList(1, trList.size());
		normalType(tr, name, signSpec, trList, sublist, retSign, oList);
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
			List<TypeRepresentation> trList, List<TypeRepresentation> sublist, String retSign, List<TypeRepresentation> oList) {
		if (subsignatures.containsKey(tr) && sublist.size() == 0)
			throw new MethodAlreadyExistException();
		
		if (retSign == null){
			retSign = tr.toJVMTypeString();
			if (subsignatures.size() == 1 && !subsignatures.containsKey(tr))
				throw new CompilationException("multiple return types");
		} else
			signSpec = signSpec + tr.toJVMTypeString();
		
		if (!subsignatures.containsKey(tr))
			subsignatures.put(tr, new FunctionSignature(name, signSpec, sublist, retSign, oList));
		else
			subsignatures.get(tr).reparse(name, signSpec, sublist, retSign, oList);
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
			return subsignatures.get(trList.get(0)).byParameters(trList.subList(1, trList.size()), false);
		}
		return null;
	}

	/**
	 * Returns all valid type specs by return
	 * @param ret
	 * @param object
	 * @return
	 */
	public List<SignatureSpecifier> byReturn(TypeRepresentation ret,
			List<SignatureSpecifier> list) {
		if (list == null){
			list = new ArrayList<SignatureSpecifier>();
			if (subsignatures.containsKey(ret))
				subsignatures.get(ret).byReturn(ret, list);
		} else {
			if (levelName != null)
				list.add(levelName);
			else
				for (TypeRepresentation tr : subsignatures.keySet())
					subsignatures.get(tr).byReturn(ret, list);
		}
		return list;
	}

	public SignatureSpecifier byParameters(List<TypeRepresentation> parameters, boolean first) {
		if (first){
			return subsignatures.values().iterator().next().byParameters(parameters, false);
		}
		if (parameters.size() == 0)
			return levelName;
		
		TypeRepresentation parameter = parameters.get(0);
		
		if (parameter.getType() == SystemTypes.CUSTOM && parameter.getFqTypeName() == null){
			// placeholder context type
			
			for (TypeRepresentation tr : subsignatures.keySet()){
				SignatureSpecifier ret = subsignatures.get(tr).byParameters(parameters.subList(1, parameters.size()), false);
				if (ret != null){
					parameter.setFqTypeName(tr.getFqTypeName());
					return ret;
				}
			}
			return null;
		}
		
		if (subsignatures.containsKey(parameter)){
			return subsignatures.get(parameter).byParameters(parameters.subList(1, parameters.size()), false);
		}
		
		return null;
	}
}
