package cz.upol.vanusanik.disindent.errors;

/**
 * For errors in creating or using build path.
 * @author Peter Vanusanik
 *
 */
public class BuildPathException extends DisindentRuntimeFailure {
	private static final long serialVersionUID = 7159382484669578216L;

	public BuildPathException(String message) {
		super(message);
	}

	public BuildPathException(Throwable t) {
		super(t);
	}

}
