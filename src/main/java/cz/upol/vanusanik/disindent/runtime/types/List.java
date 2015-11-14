package cz.upol.vanusanik.disindent.runtime.types;

/**
 * Represents linked lists of type
 * @author Peter Vanusanik
 *
 * @param <E> typeof list
 */
public class List<E> {
	/** head of the list, null if empty */
	private E head;
	/** rest of the list, null if empty */
	private List<E> rest;
	
	public List(){
		head = null;
		rest = null;
	}
	
	public List(E e){
		head = e;
		rest = null;
	}
	
	public List(E e, List<E> rest){
		head = e;
		this.rest = rest;
	}
	
	/**
	 * Makes list out of element and list
	 * @param e element
	 * @param l, maybe null
	 * @return new list containing e
	 */
	public static <T> List<T> constList(T e, List<T> l){
		if (l == null)
			return new List<T>(e);
		return new List<T>(e, l);
	}
	
	/**
	 * Returns head of the list, or null if it is empty list
	 * @param l list
	 * @return head of the list
	 */
	public static <T> T car(List<T> l){
		return l.head;
	}
	
	/**
	 * Returns rest of the list, or null if empty
	 * @param l input list
	 * @return rest or null if empty
	 */
	public static <T> List<T> cdr(List<T> l){
		return l.rest;
	}
}
