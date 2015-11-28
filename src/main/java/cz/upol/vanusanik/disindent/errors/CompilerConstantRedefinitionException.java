package cz.upol.vanusanik.disindent.errors;

public class CompilerConstantRedefinitionException
		extends DisindentRuntimeFailure {
	private static final long serialVersionUID = -6338565859305639622L;

	public CompilerConstantRedefinitionException(String message) {
		super(message);
	}

}
