package cz.upol.vanusanik.disindent.errors;

/**
 * Raised when module has two same signatured methods
 * @author Peter Vanusanik
 *
 */
public class MethodAlreadyExistException extends ModuleDefinitionError {
	private static final long serialVersionUID = -8971219490910837005L;

	public MethodAlreadyExistException() {
		super("function with that name and signature already exists");
	}

}
