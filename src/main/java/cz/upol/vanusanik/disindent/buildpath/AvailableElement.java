package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;

/**
 * Represents either typedef or module, either having fields if typedef or functions if module
 * @author pvan
 *
 */
class AvailableElement implements Comparable<AvailableElement>, Serializable {
	private static final long serialVersionUID = 7862089816130374250L;
	
	/** Whether this is typedef or moduledef */
	boolean isTypedef = false;
	/** Whether this class is valid at all. This set to false means that class uses wonky definitions, such as using module as type, which is not allowed, but is only found after build path has been fully constructed */
	boolean valid = true;
	
	/** Source filename, null for typedefs */
	String sourceName;
	/** package in . format, available to both */
	String modulePackage;
	/** package in / format, null for typedefs */
	String slashPackage;
	/** element name in java */
	String elementName;
	/** element name in din*/
	String elementDinName;
	/** source code, null for typedefs */
	byte[] source;
	
	/** parent module, null for modules */
	AvailableElement module;
	
	/** Generics stored here */
	GenericSignature genericSignature = new GenericSignature();
	/** Fields and their signatures stored here, empty for modules */
	FieldSignatures fieldSignatures = new FieldSignatures();
	/** Functions and their signatures stored here, empty for typedefs */
	FunctionSignatures functionSignatures = new FunctionSignatures();
	
	@Override
	public int compareTo(AvailableElement o) {
		int v = modulePackage.compareTo(o.modulePackage);
		if (v == 0)
			v = elementName.compareTo(o.elementName);
		return v;
	}
	
}
