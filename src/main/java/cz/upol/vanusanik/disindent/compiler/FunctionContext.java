package cz.upol.vanusanik.disindent.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.errors.CompilationException;
import cz.upol.vanusanik.disindent.errors.TypeException;

/**
 * Represents state of function at soem point of source code
 * @author Peter Vanusanik
 *
 */
public class FunctionContext {

	public final TypeRepresentation returnType;
	private int ordering = 0;
	private Stack<Map<String, TypeRepresentation>> autos = new Stack<Map<String, TypeRepresentation>>();
	private Stack<Map<String, Integer>> autoToLocal = new Stack<Map<String, Integer>>();
	
	public FunctionContext(TypeRepresentation returnType){
		this.returnType = returnType;
	}
	
	public void push(){
		autos.push(new LinkedHashMap<String, TypeRepresentation>());
		autoToLocal.push(new HashMap<String, Integer>());
	}
	
	public void addLocal(String name, TypeRepresentation type){
		if (autos.peek().containsKey(name))
			throw new TypeException("duplicite type definition");
		autos.peek().put(name, type);
		autoToLocal.peek().put(name, ordering++);
		if (type.isDoubleMemory())
			ordering++;
	}
	
	/**
	 * Returns type of the local variable
	 * @param auto
	 * @return
	 */
	public TypeRepresentation autoToType(String auto){
		TypeRepresentation tr = null;
		List<Map<String, TypeRepresentation>> cr = new ArrayList<Map<String, TypeRepresentation>>(autos);
		Collections.reverse(cr);
		
		for (Map<String, TypeRepresentation> m : cr){
			tr = m.get(auto);
			if (tr != null)
				return tr;
		}
		
		throw new CompilationException("variable " + auto + " unbound");
	}
	
	/**
	 * Returns order id of local variable
	 * @param auto
	 * @return
	 */
	public Integer autoToNum(String auto){
		Integer an = null;
		List<Map<String, Integer>> cr = new ArrayList<Map<String, Integer>>(autoToLocal);
		Collections.reverse(cr);
		
		for (Map<String, Integer> m : cr){
			an = m.get(auto);
			if (an != null)
				return an;
		}
		
		return null;
	}

	/**
	 * Pops the local scope. Happens at the end of the scope life. Will add local definition to the classdef
	 * @param mv
	 * @param start
	 */
	public void pop(MethodVisitor mv, Label start, boolean reuseOrdering){
		Label end = new Label();
        mv.visitLabel(end);
        for (String varName : autos.peek().keySet()){
        	Integer varId = autoToLocal.peek().get(varName);
        	TypeRepresentation varType = autos.peek().get(varName);
        	
        	mv.visitLocalVariable(varName, varType.toJVMTypeString(), null, start, end, varId);
        }
		autos.pop();
		Map<String, Integer> sm = autoToLocal.pop();
		
		if (reuseOrdering){
			// clean up used order indexes
			Integer min = null;
			for (Integer i : sm.values()){
				if (min == null)
					min = i;
				min = Math.min(i, min);
			}
			if (min != null)
				ordering = min;
		}
	}
}
