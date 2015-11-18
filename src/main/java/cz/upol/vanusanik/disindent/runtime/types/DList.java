package cz.upol.vanusanik.disindent.runtime.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents linked lists of type
 * @author Peter Vanusanik
 *
 * @param <E> typeof list
 */
public class DList<E> {
	/** head of the list, null if empty */
	private E head;
	/** rest of the list, null if empty */
	private DList<E> rest;
	
	public DList(){
		head = null;
		rest = null;
	}
	
	public DList(E e){
		head = e;
		rest = null;
	}
	
	public DList(E e, DList<E> rest){
		head = e;
		this.rest = rest;
	}
	
	/**
	 * Makes list out of element and list
	 * @param e element
	 * @param l, maybe null
	 * @return new list containing e
	 */
	public static <T> DList<T> constList(T e, DList<T> l){
		if (l == null)
			return new DList<T>(e);
		return new DList<T>(e, l);
	}
	
	/**
	 * Returns head of the list, or null if it is empty list
	 * @param l list
	 * @return head of the list
	 */
	public static <T> T car(DList<T> l){
		return l.head;
	}
	
	/**
	 * Returns rest of the list, or null if empty
	 * @param l input list
	 * @return rest or null if empty
	 */
	public static <T> DList<T> cdr(DList<T> l){
		return l.rest;
	}

	public Object[] toObjectArray() {
		List<Object> ol = new ArrayList<Object>();
		toOArray(ol);
		return ol.toArray();
	}

	private void toOArray(List<Object> ol) {
		if (rest == null && head == null)
			return;
		ol.add(head);
		if (rest != null)
			rest.toOArray(ol);
	}

	public static DList<Object> asList(Object[] results) {
		if (results == null || results.length == 0)
			return new DList<Object>();
		DList<Object> head = new DList<Object>();
		DList<Object> chead = head;
		for (Object o : results){
			chead.head = o;
			DList<Object> rest = new DList<Object>();
			chead.rest = rest;
			chead = rest;
		}
		chead.rest = null;
		return head;
	}

	@Override
	public String toString() {
		return "DList [head=" + head + ", rest=" + rest + "]";
	}
}
