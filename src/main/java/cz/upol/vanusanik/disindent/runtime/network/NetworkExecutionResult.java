package cz.upol.vanusanik.disindent.runtime.network;

/**
 * Holds the result of network execution, either exceptions or results are non
 * null
 * 
 * @author Peter Vanusanik
 *
 */
public class NetworkExecutionResult {
	public String[] exceptions;
	public Object[] result;

	/**
	 * Returns true if exception happened
	 * 
	 * @return
	 */
	public boolean hasExceptions() {
		if (exceptions == null)
			return false;
		return true;
	}
}
