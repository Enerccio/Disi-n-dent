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

public class CompilerUtils implements Opcodes {

	/**
	 * Returns TypeContext as TypeRepresentation
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
				throw new MalformedImportDeclarationException("type " + tt + " is not defined");

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
	 * @param typeList
	 * @return
	 */
	public static String nativeSignature(List<TypeRepresentation> typeList) {
		String sign = "(";
		String rsign = null;
		
		for (TypeRepresentation tr : typeList){
			if (rsign == null){
				rsign = ")" + tr.toNativeTypeString();
			} else {
				sign += tr.toNativeTypeString();
			}
		}
		
		return sign + rsign;
	}
	
	/**
	 * Adds correct type of load operation to stack based on argument type
	 * @param mv
	 * @param id
	 * @param type
	 */
	public static void addLoad(MethodVisitor mv, Integer id, TypeRepresentation type) {
		switch (type.getType()){
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
		switch (type.getType()){
		case DOUBLE:
			mv.visitInsn(DRETURN);
			break;
		case FLOAT:
			mv.visitInsn(FRETURN);
			break;
		case LONG:
			mv.visitInsn(IRETURN);
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
}
