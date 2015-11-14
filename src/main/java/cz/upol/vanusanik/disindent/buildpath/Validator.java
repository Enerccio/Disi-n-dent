package cz.upol.vanusanik.disindent.buildpath;

/**
 * Validates BP element once then BP is completed and removes the invalid element
 * @author Peter Vanusanik
 *
 */
public abstract class Validator {
	/** bound element to validate */
	private AvailableElement ae;
	
	public Validator(AvailableElement ae){
		this.ae = ae;
	}
	
	public void validate(BuildPath bp){
		try {
			if (!valid(bp)){
				ae.valid = false;
			}
		} catch (Exception e) {
			ae.valid = false;
		}
	}
	
	/**
	 * Whether element is still valid or not
	 * @param bp
	 * @return
	 */
	public abstract boolean valid(BuildPath bp) throws Exception;
}
