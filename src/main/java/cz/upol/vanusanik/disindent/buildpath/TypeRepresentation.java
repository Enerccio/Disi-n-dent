package cz.upol.vanusanik.disindent.buildpath;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents type for purpose of build path or compiler
 * @author Peter Vanusanik
 *
 */
public class TypeRepresentation {
	
	public enum SystemTypes {
		BOOL, INT, FLOAT, LONG, DOUBLE, STRING, FUNCTION, COMPLEX, CUSTOM, ANY,
	}
	
	public TypeRepresentation(){
		
	}
	
	private TypeRepresentation(SystemTypes t){
		type = t;
	}
	
	public static final TypeRepresentation BOOL = new TypeRepresentation(SystemTypes.BOOL);
	public static final TypeRepresentation INT = new TypeRepresentation(SystemTypes.INT);
	public static final TypeRepresentation LONG = new TypeRepresentation(SystemTypes.LONG);
	public static final TypeRepresentation FLOAT = new TypeRepresentation(SystemTypes.FLOAT);
	public static final TypeRepresentation DOUBLE = new TypeRepresentation(SystemTypes.DOUBLE);
	public static final TypeRepresentation STRING = new TypeRepresentation(SystemTypes.STRING);
	public static final TypeRepresentation FUNCTION = new TypeRepresentation(SystemTypes.FUNCTION);
	public static final TypeRepresentation ANY = new TypeRepresentation(SystemTypes.ANY);
	
	private SystemTypes type;
	private TypeRepresentation simpleType;
	private String fqTypeName;
	
	public SystemTypes getType() {
		return type;
	}
	
	public void setType(SystemTypes type) {
		this.type = type;
	}
	
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
	
	public boolean isComplexType(){
		return simpleType != null;
	}
	
	public boolean isCustomType(){
		return fqTypeName != null && StringUtils.isAllLowerCase(fqTypeName.subSequence(0, 1));
	}
	
	public boolean isGenericSpecifier(){
		return fqTypeName != null && StringUtils.isAllUpperCase(fqTypeName);
	}
}
