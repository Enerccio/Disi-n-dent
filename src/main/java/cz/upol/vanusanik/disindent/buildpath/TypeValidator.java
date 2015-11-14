package cz.upol.vanusanik.disindent.buildpath;

/**
 * Validates whether the type actually exists
 * @author Peter Vanusanik
 *
 */
public class TypeValidator extends Validator {

	/** TypeRepresentation to validate*/
	private TypeRepresentation tr;
	
	public TypeValidator(AvailableElement mae, TypeRepresentation tr) {
		super(mae);
		this.tr = tr;
	}

	@Override
	public boolean valid(BuildPath bp) throws Exception {
		AvailableElement ae = bp.bpElements.get(tr.getFqTypeName());
		if (ae == null || !ae.valid)
			return false;
		return true;
	}

}
