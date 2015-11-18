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
	public Object[] results;
	public String[] exceptionMessages;

	/**
	 * Returns true if exception happened
	 * 
	 * @return
	 */
	public boolean hasExceptions() {
		if (exceptions == null)
			return false;
		for (String e : exceptions)
			if (e != null)
				return true;
		return false;
	}
}
