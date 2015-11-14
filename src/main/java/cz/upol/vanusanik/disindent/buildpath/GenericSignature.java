package cz.upol.vanusanik.disindent.buildpath;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds information about generics for the typename.
 * @author Peter Vanusanik
 *
 */
public class GenericSignature implements Serializable {
	private static final long serialVersionUID = -4750146102090334638L;

	/** Generics defined here */
	private Set<String> generics = new HashSet<String>();
	
	/**
	 * Adds generic to typedef declaration
	 * @param generic
	 */
	public void addGeneric(String generic){
		generics.add(generic);
	}
	
	/**
	 * @param generic
	 * @return whether the generics is present or not with that name
	 */
	public boolean isGeneric(String generic){
		return generics.contains(generic);
	}
	
	/**
	 * Returns iterable to all generics defined
	 * @return
	 */
	public Iterable<String> getDefinedGenerics(){
		return generics;
	}

	/**
	 * @return number of generic parameters
	 */
	public int number() {
		return generics.size();
	}
}
