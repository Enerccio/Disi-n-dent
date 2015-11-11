package cz.upol.vanusanik.disindent.errors;

/**
 * General runtime failure in disindent
 * @author Peter Vanusanik
 *
 */
public class DisindentRuntimeFailure extends RuntimeException {
	private static final long serialVersionUID = 8969344878121459725L;
	
	public DisindentRuntimeFailure(Throwable t) {
		super(t);
	}
	
	public DisindentRuntimeFailure(String message) {
		super(message);
	}
}
