package cz.upol.vanusanik.disindent.errors;

/**
 * Thrown when build path module has no valid name
 * @author Peter Vanusanik
 *
 */
public class BuildPathModuleNameIncorrectException extends BuildPathException {
	private static final long serialVersionUID = 748878558514473799L;

	public BuildPathModuleNameIncorrectException(String message) {
		super(message);
	}

}
