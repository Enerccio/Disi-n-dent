package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents collection of types and names of the types 
 * @author Peter Vanusanik
 *
 */
public class FieldSignatures implements Serializable {
	private static final long serialVersionUID = 3880945549157153280L;

	/** Fields stored here */
	private Map<String, TypeRepresentation> fields = new HashMap<String, TypeRepresentation>();
	
	/**
	 * Adds field for this typename
	 * @param fieldName name of the field
	 * @param type type of the field
	 */
	public void addField(String fieldName, TypeRepresentation type){
		fields.put(fieldName, type);
	}
	
	/**
	 * Returns type name for field name.
	 * @param fieldName
	 * @return
	 */
	public TypeRepresentation getType(String fieldName){
		return fields.get(fieldName);
	}

	/**
	 * Returns all fields defined in this typedef
	 * @return
	 */
	public Iterable<String> getFields() {
		return fields.keySet();
	}

	public boolean containsField(String baseName) {
		return fields.containsKey(baseName);
	}
}
