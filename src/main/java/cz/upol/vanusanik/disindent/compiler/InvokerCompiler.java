package cz.upol.vanusanik.disindent.compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;

public class InvokerCompiler implements Opcodes {
	
	private DisindentClassLoader cl;
	private String requestedType;
	private BuildPath bp = BuildPath.getBuildPath();
	
	public InvokerCompiler(DisindentClassLoader loader, String requestedType){
		this.requestedType = requestedType.substring(BuildPath.INVOKER_NAME_LENGTH);
	}
	
	public void compile(){
		compile(BuildPath.INVOKER_BASE_NAME, asTypes(requestedType));
	}

	private void compile(String invokerBaseName, List<TypeRepresentation> types) {
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
				| ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, invokerBaseName + requestedType, null,
				"java/lang/Object", null);
		cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "invoke", generateDescription(types), null, null).visitEnd();
		cw.visitEnd();
		
		byte[] classData = cw.toByteArray();
		
		PrintWriter pw = new PrintWriter(System.out);
		try {
			CheckClassAdapter.verify(new ClassReader(classData), cl, true, pw);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		cl.addClass(invokerBaseName + requestedType, classData);
	}

	private String generateDescription(List<TypeRepresentation> types) {
		TypeRepresentation returnType = types.get(0);
		types.remove(0);
		String description = "(";
		for (TypeRepresentation tr : types){
			description += tr.toJVMTypeString();
		}
		description += ")" + returnType.toJVMTypeString();
		return description;
	}

	private List<TypeRepresentation> asTypes(String requestedType) {
		String[] identificators = StringUtils.split(requestedType);
		List<TypeRepresentation> trList = new ArrayList<TypeRepresentation>();
		
		for (String identificator : identificators){
			trList.add(asType(identificator));
		}
		
		return trList;
	}

	private TypeRepresentation asType(String identificator) {
		TypeRepresentation tr;
		switch (identificator.charAt(0)){
		case 'L': {
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(asType(identificator.substring(1)));
			return tr;
		} 
		case 'C': {
			return bp.getOrderType(Integer.parseInt(identificator.substring(1)));
		}
		case 'A': return TypeRepresentation.ANY;
		case 'Z': return TypeRepresentation.BOOL;
		case 'B': return TypeRepresentation.BYTE;
		case 'S': return TypeRepresentation.SHORT;
		case 'D': return TypeRepresentation.DOUBLE;
		case 'F': return TypeRepresentation.FLOAT;
		case 'f': {
			return bp.getOrderType(Integer.parseInt(identificator.substring(1)));
		}
		case 'I':return TypeRepresentation.INT;
		case 'J':return TypeRepresentation.LONG;
		case 's':
		default: return TypeRepresentation.STRING;
		}
		
	}

}
