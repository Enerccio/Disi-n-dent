package cz.upol.vanusanik.disindent.compiler;

import java.util.List;
import java.util.Map;

import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Base_typeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Const_argContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypeContext;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.errors.TypeException;
import cz.upol.vanusanik.disindent.utils.Utils;

public class CompilerUtils implements Opcodes {

	/**
	 * Returns TypeContext as TypeRepresentation
	 * 
	 * @param type
	 * @return
	 */
	public static TypeRepresentation asType(TypeContext tc,
			Map<String, String> imports) {
		TypeRepresentation tr = null;
		Base_typeContext base = tc.base_type();

		if (base.simple_type() != null) {
			tr = TypeRepresentation.asSimpleType(base.simple_type().getText());
		} else if (base.function_type() != null) {
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.FUNCTION);
			for (TypeContext innerType : base.function_type().type()) {
				tr.addGenerics(asType(innerType, imports));
			}
		} else {
			String type = base.complex_identifier().getText().replace("::",
					".");
			String fqType = asFqType(type, imports);

			tr = new TypeRepresentation();
			tr.setType(SystemTypes.CUSTOM);
			tr.setFqTypeName(fqType);
		}

		for (int i = 0; i < tc.multiplier().size(); i++) {
			TypeRepresentation oldr = tr;
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(oldr);
		}

		return tr;
	}

	/**
	 * Returns native signature for types
	 * 
	 * @param typeList
	 * @return
	 */
	public static String nativeSignature(List<TypeRepresentation> typeList) {
		String sign = "(";
		String rsign = null;

		for (TypeRepresentation tr : typeList) {
			if (rsign == null) {
				rsign = ")" + tr.toNativeTypeString();
			} else {
				sign += tr.toNativeTypeString();
			}
		}

		return sign + rsign;
	}

	/**
	 * Adds correct type of load operation to stack based on argument type
	 * 
	 * @param mv
	 * @param id
	 * @param type
	 */
	public static void addLoad(MethodVisitor mv, Integer id,
			TypeRepresentation type) {
		switch (type.getType()) {
		case DOUBLE:
			mv.visitVarInsn(DLOAD, id);
			break;
		case FLOAT:
			mv.visitVarInsn(FLOAD, id);
			break;
		case LONG:
			mv.visitVarInsn(LLOAD, id);
			break;
		case BYTE:
		case BOOL:
		case INT:
		case SHORT:
			mv.visitVarInsn(ILOAD, id);
			break;
		case ANY:
		case COMPLEX:
		case CUSTOM:
		case CONSTRUCTABLE:
		case FUNCTION:
		case CALLABLE:
		case STRING:
			mv.visitVarInsn(ALOAD, id);
			break;
		default:
			break;
		}
	}

	/**
	 * Adds correct type of store operation to stack based on argument type
	 * 
	 * @param mv
	 * @param id
	 * @param type
	 */
	public static void addStore(MethodVisitor mv, Integer id,
			TypeRepresentation type) {
		switch (type.getType()) {
		case DOUBLE:
			mv.visitVarInsn(DSTORE, id);
			break;
		case FLOAT:
			mv.visitVarInsn(FSTORE, id);
			break;
		case LONG:
			mv.visitVarInsn(LSTORE, id);
			break;
		case BYTE:
		case BOOL:
		case INT:
		case SHORT:
			mv.visitVarInsn(ISTORE, id);
			break;
		case ANY:
		case COMPLEX:
		case CUSTOM:
		case FUNCTION:
		case STRING:
		case CONSTRUCTABLE:
		case CALLABLE:
			mv.visitVarInsn(ASTORE, id);
			break;
		}
	}

	public static void addReturn(MethodVisitor mv, TypeRepresentation type) {
		switch (type.getType()) {
		case DOUBLE:
			mv.visitInsn(DRETURN);
			break;
		case FLOAT:
			mv.visitInsn(FRETURN);
			break;
		case LONG:
			mv.visitInsn(LRETURN);
			break;
		case BYTE:
		case BOOL:
		case INT:
		case SHORT:
			mv.visitInsn(IRETURN);
			break;
		case ANY:
		case COMPLEX:
		case CUSTOM:
		case FUNCTION:
		case STRING:
		case CONSTRUCTABLE:
		case CALLABLE:
			mv.visitInsn(ARETURN);
			break;
		}
	}

	/**
	 * Creates cast between tr and as type, if possible, if not throws exception
	 * 
	 * @param mv
	 * @param tr
	 * @param as
	 */
	public static void congruentCast(MethodVisitor mv, TypeRepresentation tr,
			TypeRepresentation as) {
		if (as.getType() == SystemTypes.ANY) {
			if (tr.isComplexType() || tr.isCustomType()) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Object");
			} else {
				switch (tr.getType()) {
				case BOOL:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean",
							"valueOf", "(Z)Ljava/lang/Boolean;", false);
					break;
				case BYTE:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte",
							"valueOf", "(B)Ljava/lang/Byte;", false);
					break;
				case DOUBLE:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double",
							"valueOf", "(D)Ljava/lang/Double;", false);
					break;
				case FLOAT:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float",
							"valueOf", "(F)Ljava/lang/Float;", false);
					break;
				case INT:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer",
							"valueOf", "(I)Ljava/lang/Integer;", false);
					break;
				case LONG:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long",
							"valueOf", "(J)Ljava/lang/Long;", false);
					break;
				case SHORT:
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short",
							"valueOf", "(S)Ljava/lang/Short;", false);
					break;
				default:
				case FUNCTION:
				case ANY:
				case STRING:
					break;
				}
				mv.visitTypeInsn(CHECKCAST, "java/lang/Object");
			}
			return; // any always succeeds
		} else if (as.isComplexType()) {
			if (as.equals(tr))
				return;
			// complex types only succeed if they the same, ie cast was not
			// necessary
		} else if (as.isCustomType()) {
			if (as.equals(tr))
				return;
			// custom types only succeed if they are both custom, ie cast was
			// not necessary
		} else {
			switch (as.getType()) {
			case ANY:
			case COMPLEX:
			case CUSTOM:
			case CONSTRUCTABLE:
			case CALLABLE:
				break;

			case BOOL:
			case INT:
			case BYTE:
			case SHORT:
				switch (tr.getType()) {
				case BOOL:
				case BYTE:
				case INT:
				case SHORT:
					return;
				case DOUBLE:
					mv.visitInsn(D2I);
					return;
				case FLOAT:
					mv.visitInsn(F2I);
					return;
				case LONG:
					mv.visitInsn(L2I);
					return;
				case ANY:
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number",
							"intValue", "()I", false);
					return;
				default:
					break;
				}
			case DOUBLE:
				switch (tr.getType()) {
				case BOOL:
				case BYTE:
				case INT:
				case SHORT:
					mv.visitInsn(I2D);
					return;
				case DOUBLE:
					return;
				case FLOAT:
					mv.visitInsn(F2D);
					return;
				case LONG:
					mv.visitInsn(L2D);
					return;
				case ANY:
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number",
							"doubleValue", "()D", false);
					return;
				default:
					break;
				}
			case FLOAT:
				switch (tr.getType()) {
				case BOOL:
				case BYTE:
				case INT:
				case SHORT:
					mv.visitInsn(I2F);
					return;
				case DOUBLE:
					mv.visitInsn(D2F);
					return;
				case FLOAT:
					return;
				case LONG:
					mv.visitInsn(L2F);
					return;
				case ANY:
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number",
							"floatValue", "()F", false);
					return;
				default:
					break;
				}
				break;
			case LONG:
				switch (tr.getType()) {
				case BOOL:
				case BYTE:
				case INT:
				case SHORT:
					mv.visitInsn(I2L);
					return;
				case DOUBLE:
					mv.visitInsn(D2L);
					return;
				case FLOAT:
					mv.visitInsn(F2L);
					return;
				case LONG:
					return;
				case ANY:
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number",
							"longValue", "()J", false);
					return;
				default:
					break;
				}
				break;

			case STRING:
				if (tr.getType() == SystemTypes.STRING)
					return;
				break;
			case FUNCTION:
				if (tr.getType() == SystemTypes.FUNCTION)
					return;
				break;
			}
		}

		throw new TypeException("Types are incogruent for cast or implied cast "
				+ tr + " to " + as);
	}

	/**
	 * Returns true if types are congruent. Only calls equals and handles
	 * undefined reference type (null as atom for instance)
	 * 
	 * @param test
	 * @param requiredType
	 * @return
	 */
	public static boolean congruentType(MethodVisitor mv,
			TypeRepresentation test, TypeRepresentation requiredType) {
		if (test == TypeRepresentation.NULL) {
			if (requiredType.isComplexType() || requiredType.isCustomType()
					|| requiredType.getType() == SystemTypes.ANY
					|| requiredType.getType() == SystemTypes.FUNCTION
					|| requiredType.getType() == SystemTypes.STRING)
				return true;
		}
		boolean eqType = test.equals(requiredType);
		if (eqType)
			return true;

		if (requiredType == TypeRepresentation.LONG) {
			switch (test.getType()) {
			case BOOL:
			case BYTE:
			case INT:
			case SHORT:
				mv.visitLdcInsn(I2L);
				return true;
			default:
				break;

			}
		}

		if (requiredType == TypeRepresentation.DOUBLE) {
			switch (test.getType()) {
			case FLOAT:

				return true;
			default:
				break;

			}
		}

		return false;
	}

	/**
	 * Default value for type
	 * 
	 * @param mv
	 * @param type
	 */
	public static void defaultValue(MethodVisitor mv, TypeRepresentation type) {
		if (type.isComplexType() || type.isCustomType()
				|| type.getType() == SystemTypes.ANY
				|| type.getType() == SystemTypes.FUNCTION
				|| type.getType() == SystemTypes.STRING) {
			mv.visitInsn(ACONST_NULL);
			return;
		}

		switch (type.getType()) {
		case BOOL:
			mv.visitInsn(ICONST_0);
			break;
		case BYTE:
			mv.visitInsn(ICONST_0);
			break;
		case DOUBLE:
			mv.visitInsn(DCONST_0);
			break;
		case FLOAT:
			mv.visitInsn(FCONST_0);
			break;
		case INT:
			mv.visitInsn(ICONST_0);
			break;
		case LONG:
			mv.visitInsn(LCONST_0);
			break;
		case SHORT:
			mv.visitInsn(ICONST_0);
			break;
		case ANY:
		case COMPLEX:
		case CUSTOM:
		case FUNCTION:
		case STRING:
		default:
			break;
		}
	}

	/**
	 * Returns jump type for operation, this jump type must jump when true
	 * 
	 * @param operation
	 * @return
	 */
	public static int cmpChoice(String operation) {
		switch (operation) {
		case "=":
			return IF_ICMPEQ;
		case "<>":
			return IF_ICMPNE;
		case ">":
			return IF_ICMPGT;
		case "<":
			return IF_ICMPLT;
		case ">=":
			return IF_ICMPLE;
		case "<=":
			return IF_ICMPGE;
		}
		return 0;
	}

	public static void addFieldLoad(MethodVisitor mv, String accessor,
			TypeRepresentation varType, TypeRepresentation contextType) {
		mv.visitFieldInsn(GETFIELD,
				contextType.toJVMTypeString().substring(1,
						contextType.toJVMTypeString().length() - 1),
				accessor, varType.toJVMTypeString());
	}

	public static Object asValue(Const_argContext c) {
		if (c.getText().equals("none")) {
			return null;
		}

		if (c.getText().equals("true")) {
			return Boolean.TRUE;
		}

		if (c.getText().equals("false")) {
			return Boolean.FALSE;
		}

		if (c.IntegerConstant() != null) {
			// integer
			String text = c.getText();

			int iv;
			if (text.length() == 1)
				iv = Integer.parseInt(text);
			else if (text.charAt(0) == '0' && text.contains("b"))
				iv = Integer.parseInt(text.substring(2), 2);
			else if (text.charAt(0) == '0' && text.contains("x"))
				iv = Integer.parseInt(text.substring(2), 16);
			else if (text.charAt(0) == '0')
				iv = Integer.parseInt(text.substring(1), 8);
			else
				iv = Integer.parseInt(text);

			return iv;
		}

		if (c.LongConstant() != null) {
			// long
			String text = c.getText();
			text = text.substring(0, text.length() - 1);

			long lv;
			if (text.length() == 1)
				lv = Long.parseLong(text);
			else if (text.charAt(0) == '0' && text.contains("b"))
				lv = Long.parseLong(text.substring(2), 2);
			else if (text.charAt(0) == '0' && text.contains("x"))
				lv = Long.parseLong(text.substring(2), 16);
			else if (text.charAt(0) == '0')
				lv = Long.parseLong(text.substring(1), 8);
			else
				lv = Long.parseLong(text);

			return lv;
		}

		if (c.DoubleConstant() != null) {
			// double
			String text = c.getText();
			double dv = Double.parseDouble(text);
			return dv;
		}

		if (c.FloatConstant() != null) {
			// float
			String text = c.getText();
			text = text.substring(0, text.length() - 1);
			float fv = Float.parseFloat(text);
			return fv;
		}

		if (c.String() != null) {
			// string
			String strValue = c.getText();
			strValue = Utils.removeEscapes(strValue);
			strValue = strValue.substring(1, strValue.length() - 1);
			return strValue;
		}

		return null;
	}

	public static String asFqType(String type, Map<String, String> imports) {
		String fqType = imports.get(type);
		if (fqType == null) {
			for (String ikey : imports.keySet()) {
				String ivalue = imports.get(ikey);
				String combination = Utils.combine(ivalue, type, ".");
				if (combination != null) {
					fqType = combination;
					break;
				}
			}
			if (fqType == null)
				fqType = type;
		}
		return fqType;

	}
}
