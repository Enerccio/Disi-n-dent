package cz.upol.vanusanik.disindent.errors;

public class MalformedImportDeclarationException extends
		DisindentRuntimeFailure {
	private static final long serialVersionUID = -337197782515446074L;

	public MalformedImportDeclarationException(String message) {
		super(message);
	}

}
