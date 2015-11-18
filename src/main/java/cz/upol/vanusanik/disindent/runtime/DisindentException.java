package cz.upol.vanusanik.disindent.runtime;

public class DisindentException extends RuntimeException {
	private static final long serialVersionUID = 2821154904880756984L;
	
	private String identifier;
	public DisindentException(String identifier, String message){
		super(message);
		this.identifier = identifier;
	}

	public String getIndentifier() {
		return identifier;
	}

}
