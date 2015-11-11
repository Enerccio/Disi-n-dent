package cz.upol.vanusanik.disindent.errors;

/**
 * Thrown if compilation of din file fails at any point
 * @author Peter Vanusanik
 *
 */
public class CompilationException extends DisindentRuntimeFailure {
	private static final long serialVersionUID = -7511230903388609338L;

	public CompilationException(Throwable t) {
		super(t);
	}
	
	public CompilationException(String message) {
		super(message);
	}

}
