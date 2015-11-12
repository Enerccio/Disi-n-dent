package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;

/**
 * Represents either typedef or module, either having fields if typedef or functions if module
 * @author pvan
 *
 */
class AvailableElement implements Comparable<AvailableElement>, Serializable {
	private static final long serialVersionUID = 7862089816130374250L;
	
	String sourceName;
	String modulePackage;
	String elementName;
	byte[] source;
	
	AvailableElement module;
	
	FieldSignatures fieldSignatures = new FieldSignatures();
	FunctionSignatures functionSignatures = new FunctionSignatures();
	
	@Override
	public int compareTo(AvailableElement o) {
		int v = modulePackage.compareTo(o.modulePackage);
		if (v == 0)
			v = elementName.compareTo(o.elementName);
		return v;
	}
	
}
