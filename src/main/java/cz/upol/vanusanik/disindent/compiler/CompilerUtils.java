package cz.upol.vanusanik.disindent.compiler;

import java.util.List;

import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ListContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypepartContext;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.errors.MalformedImportDeclarationException;
import cz.upol.vanusanik.disindent.errors.TypeException;

public class CompilerUtils implements Opcodes {

	/**
	 * Returns TypeContext as TypeRepresentation
	 * 
	 * @param type
	 * @return
	 */
	public static TypeRepresentation asType(TypeContext type, Imports imports) {
		TypepartContext typepc = type.typepart();

		String tt;
		if (typepc.fqName() != null)
			tt = typepc.fqName().getText();
		else
			tt = typepc.getText();

		TypeRepresentation tr = null;

		tr = TypeRepresentation.asSimpleType(tt);
		if (tr == null) {
			String fqPath = imports.importMapOriginal.get(tt);
			if (fqPath == null)
				throw new MalformedImportDeclarationException("type " + tt
						+ " is not defined");

			tr = new TypeRepresentation();
			tr.setType(SystemTypes.CUSTOM);
			tr.setFqTypeName(fqPath);
		}

		for (@SuppressWarnings("unused")
		ListContext lc : type.list()) {
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
		case FUNCTION:
		case STRING:
			mv.visitVarInsn(ALOAD, id);
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
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
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
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
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
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
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
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
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

		throw new TypeException(
				"Types are incogruent for cast or implied cast " + tr + " to "
						+ as);
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
			TypeRepresentation test,
			TypeRepresentation requiredType) {
		if (test == TypeRepresentation.NULL) {
			if (requiredType.isComplexType() || requiredType.isCustomType()
					|| requiredType.getType() == SystemTypes.ANY
					|| requiredType.getType() == SystemTypes.FUNCTION
					|| requiredType.getType() == SystemTypes.STRING)
				return true;
		}
		return test.equals(requiredType);
	}

	public static void defaultValue(MethodVisitor mv, TypeRepresentation type) {
		if (type.isComplexType() || type.isCustomType()
				|| type.getType() == SystemTypes.ANY
				|| type.getType() == SystemTypes.FUNCTION
				|| type.getType() == SystemTypes.STRING){
			mv.visitInsn(ACONST_NULL);
			return;
		}
		
		switch (type.getType()){
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
}
