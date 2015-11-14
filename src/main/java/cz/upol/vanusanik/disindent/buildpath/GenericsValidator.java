package cz.upol.vanusanik.disindent.buildpath;

/**
 * Validates whether the type actually accepts that number of generics
 * @author Peter Vanusanik
 *
 */
public class GenericsValidator extends Validator {

	/** TypeRepresentation to validate*/
	private TypeRepresentation tr;
	
	public GenericsValidator(AvailableElement mae, TypeRepresentation tr) {
		super(mae);
		this.tr = tr;
	}

	@Override
	public boolean valid(BuildPath bp) throws Exception {
		int gc = tr.getGenerics().size();
		AvailableElement ae = bp.bpElements.get(tr.getFqTypeName());
		if (ae == null || !ae.valid)
			return false;
		return ae.genericSignature.number() == gc;
	}

}
