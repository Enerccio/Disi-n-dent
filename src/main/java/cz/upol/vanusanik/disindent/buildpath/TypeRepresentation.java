package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.utils.Utils;

/**
 * Represents type for purpose of build path or compiler
 * @author Peter Vanusanik
 *
 */
public class TypeRepresentation implements Serializable {
	private static final long serialVersionUID = -7576057474510165453L;

	/**
	 * Represents system types enum
	 * @author Peter Vanusanik
	 *
	 */
	public enum SystemTypes {
		BOOL, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE, 
		STRING, FUNCTION, COMPLEX, CUSTOM, ANY, 
	}
	
	public TypeRepresentation(){
		
	}
	
	private TypeRepresentation(SystemTypes t){
		type = t;
	}
	
	public static final TypeRepresentation BOOL = new TypeRepresentation(SystemTypes.BOOL);
	public static final TypeRepresentation BYTE = new TypeRepresentation(SystemTypes.BYTE);
	public static final TypeRepresentation SHORT = new TypeRepresentation(SystemTypes.SHORT);
	public static final TypeRepresentation INT = new TypeRepresentation(SystemTypes.INT);
	public static final TypeRepresentation LONG = new TypeRepresentation(SystemTypes.LONG);
	public static final TypeRepresentation FLOAT = new TypeRepresentation(SystemTypes.FLOAT);
	public static final TypeRepresentation DOUBLE = new TypeRepresentation(SystemTypes.DOUBLE);
	public static final TypeRepresentation STRING = new TypeRepresentation(SystemTypes.STRING);
	public static final TypeRepresentation FUNCTION = new TypeRepresentation(SystemTypes.FUNCTION);
	public static final TypeRepresentation ANY = new TypeRepresentation(SystemTypes.ANY);
	public static final TypeRepresentation NULL = new TypeRepresentation(null);;
	
	static Map<String, TypeRepresentation> simpleTypeMap
		= new HashMap<String, TypeRepresentation>();
	
	static {
		simpleTypeMap.put("bool", BOOL);
		simpleTypeMap.put("byte", BYTE);
		simpleTypeMap.put("short", SHORT);
		simpleTypeMap.put("int", INT);
		simpleTypeMap.put("long", LONG);
		simpleTypeMap.put("float", FLOAT);
		simpleTypeMap.put("double", DOUBLE);
		simpleTypeMap.put("string", STRING);
		simpleTypeMap.put("function", FUNCTION);
		simpleTypeMap.put("any", ANY);
	}
	
	/**Type enum*/
	private SystemTypes type;
	/** If it is complex type, ie array, type is stored here */
	private TypeRepresentation simpleType;
	/** FQ type name */
	private String fqTypeName;
	/** Represents generics if assigned to the type */
	private List<TypeRepresentation> generics = new ArrayList<TypeRepresentation>();
	
	public SystemTypes getType() {
		return type;
	}
	
	public void setType(SystemTypes type) {
		this.type = type;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fqTypeName == null) ? 0 : fqTypeName.hashCode());
		result = prime * result
				+ ((generics == null) ? 0 : generics.hashCode());
		result = prime * result
				+ ((simpleType == null) ? 0 : simpleType.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeRepresentation other = (TypeRepresentation) obj;
		if (fqTypeName == null) {
			if (other.fqTypeName != null)
				return false;
		} else if (!fqTypeName.equals(other.fqTypeName))
			return false;
		if (generics == null) {
			if (other.generics != null)
				return false;
		} else if (!generics.equals(other.generics))
			return false;
		if (simpleType == null) {
			if (other.simpleType != null)
				return false;
		} else if (!simpleType.equals(other.simpleType))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "TypeRepresentation [type=" + type + ", simpleType="
				+ simpleType + ", fqTypeName=" + fqTypeName + "]";
	}

	/**
	 * @return simple type representation or null if it is not a complex type
	 */
	public TypeRepresentation getSimpleType() {
		return simpleType;
	}
	
	public void setSimpleType(TypeRepresentation simpleType) {
		this.simpleType = simpleType;
	}
	
	public String getFqTypeName() {
		return fqTypeName;
	}
	
	public void setFqTypeName(String fqTypeName) {
		this.fqTypeName = fqTypeName;
	}
	
	/**
	 * @return whether this type is complex type
	 */
	public boolean isComplexType(){
		return simpleType != null;
	}
	
	/**
	 * @return whether this type is custom type 
	 */
	public boolean isCustomType(){
		return fqTypeName != null && (fqTypeName.contains(".") || !StringUtils.isAllLowerCase(fqTypeName.subSequence(0, 1)));
	}
	
	/**
	 * Returns java type specifier as per jvm specs
	 * @return jvm version
	 */
	public String toJVMTypeString(){
		if (isCustomType()){
			String[] components = StringUtils.split(fqTypeName, ".");
			String typename = components[components.length-1];
			String moduleName = components[components.length-2];
			List<String> sl = new ArrayList<String>();
			for (int i=0; i<components.length-2; i++){
				sl.add(components[i]);
			}
			String pp = sl.size() == 0 ? "" : StringUtils.join(sl, "/");
			return "L" + (pp.equals("") ? "" : pp + "/") + 
					Utils.asModuledefJavaName(moduleName) + "$" + 
					Utils.asTypedefJavaName(typename) + ";";
		}
		
		if (isComplexType())
			return "Lcz/upol/vanusanik/disindent/runtime/types/DList;";
		
		switch (type){
		case ANY:
			return "Ljava/lang/Object;";
		case BOOL:
			return "Z";
		case BYTE:
			return "B";
		case SHORT:
			return "S";
		case DOUBLE:
			return "D";
		case FLOAT:
			return "F";
		case FUNCTION:
			return "Lcz/upol/vanusanik/disindent/runtime/types/Method;";
		case INT:
			return "I";
		case LONG:
			return "J";
		default:
		case STRING:
			return "Ljava/lang/String;";
		}
	}

	/**
	 * Return simple type from name or null if not simple type
	 * @param type
	 * @return type representation or null
	 */
	public static TypeRepresentation asSimpleType(String type) {
		return simpleTypeMap.get(type);
	}
	
	/**
	 * Adds generic parameter to the type
	 * @param tr type representation
	 */
	public void addGenerics(TypeRepresentation tr){
		generics.add(tr);
	}
	
	/**
	 * Returns all defined generic types
	 */
	public List<TypeRepresentation> getGenerics(){
		return generics;
	}

	/**
	 * Returns external java type for this type
	 * @return
	 */
	public String toJaveTypeString() {
		if (isCustomType()){
			return "Object";
		}
		
		if (isComplexType()){
			String stype = getSimpleType().toJaveTypeString();
			if (stype.equals("int"))
				stype = "integer";
			if (stype.equals("bool"))
				stype = "boolean";
			stype = StringUtils.capitalize(stype);
			return String.format("DList<%s>", stype);
		}
		
		switch (type){
		case ANY:
			return "Object";
		case BOOL:
			return "boolean";
		case BYTE:
			return "byte";
		case SHORT:
			return "short";
		case DOUBLE:
			return "double";
		case FLOAT:
			return "float";
		case FUNCTION:
			return "Method";
		case INT:
			return "int";
		case LONG:
			return "long";
		default:
		case STRING:
			return "String";
		}
	}
	
	/**
	 * Returns native java type for this type
	 * @return
	 */
	public String toNativeTypeString() {
		if (isCustomType())
			return "Ljava/lang/Object;";
		
		if (isComplexType())
			return "Lcz/upol/vanusanik/disindent/runtime/types/DList;";
		
		switch (type){
		case ANY:
			return "Ljava/lang/Object;";
		case BOOL:
			return "Z";
		case BYTE:
			return "B";
		case SHORT:
			return "S";
		case DOUBLE:
			return "D";
		case FLOAT:
			return "F";
		case FUNCTION:
			return "Ljava/lang/reflect/Method;";
		case INT:
			return "I";
		case LONG:
			return "J";
		default:
		case STRING:
			return "Ljava/lang/String;";
		}
	}
}
