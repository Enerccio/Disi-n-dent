package cz.upol.vanusanik.disindent.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation;
import cz.upol.vanusanik.disindent.errors.CompilationException;

/**
 * Represents state of function at some point of source code
 * @author Peter Vanusanik
 *
 */
public class FunctionContext {
	
	public static class FunctionBlock {
		private int ordering = 0;
		private int cordering = 0;
		
		private TypeRepresentation returnType;
		TypeRepresentation contextType;
		
		private List<ContextBlock> scopes = new ArrayList<ContextBlock>();
		Map<String, TypeRepresentation> closureVariable = new HashMap<String, TypeRepresentation>();
		Map<String, String> closureAccessor = new HashMap<String, String>();
	}
	
	public static class ContextBlock {
		private Map<String, Integer> autoNumber = new HashMap<String, Integer>();
		private Map<String, TypeRepresentation> type = new HashMap<String, TypeRepresentation>();
		
		Map<String, Integer> savedClosures = new HashMap<String, Integer>();
		Map<String, String> savedClosuresLocals = new HashMap<String, String>();
		
		FunctionBlock pFuncBlock;
	}
	
	private List<FunctionBlock> ctxStack = new ArrayList<FunctionBlock>();
	
	public FunctionContext(){
		
	}
	
	public void pushNewFunc(TypeRepresentation returnType, TypeRepresentation contextType){
		FunctionBlock fb = new FunctionBlock();
		fb.returnType = returnType;
		fb.contextType = contextType;
		if (ctxStack.size() > 0)
			ctxStack.get(0).scopes.get(0).pFuncBlock = fb;
		ctxStack.add(fb);
	}
	
	public void push(){
		ContextBlock b = new ContextBlock();
		ctxStack.get(0).scopes.add(0, b);
	}
	
	public void addLocal(String name, TypeRepresentation type){
		ctxStack.get(0).scopes.get(0).type.put(name, type);
		ctxStack.get(0).scopes.get(0).autoNumber.put(name, ctxStack.get(0).ordering++);
		if (type.isDoubleMemory())
			++ctxStack.get(0).ordering;
	}
	

	public void addField(String element, String accessor, TypeRepresentation type) {
		ctxStack.get(0).closureAccessor.put(element, accessor);
		ctxStack.get(0).closureVariable.put(element, type);
	}
	
	public boolean isLocal(String name){
		return isLocal(name, ctxStack);
	}
	
	private boolean isLocal(String name, List<FunctionBlock> ctxStack) {
		if (ctxStack.size() == 0)
			throw new CompilationException("variable " + name + " unbound");
		
		FunctionBlock b = ctxStack.get(0);
		if (b.closureVariable.containsKey(name))
			return false;
		else if (isLocalScope(name, b.scopes)){
			return true;
		} else {
			List<FunctionBlock> subList = ctxStack.size() == 1 ? new ArrayList<FunctionBlock>() : ctxStack.subList(1, ctxStack.size()-1);
			TypeRepresentation type = variableToType(name, subList.get(0));
			if (isLocal(name, subList)){
				int localId = localToAuto(name, subList.get(0));
				String accessName = name + "$" + b.cordering++;
				b.closureAccessor.put(name, accessName);
				b.closureVariable.put(name, type);
				subList.get(0).scopes.get(0).savedClosures.put(name, localId);
			} else {
				String localName = localToField(name, subList.get(0));
				String accessName = localName + "$" + b.cordering++;
				b.closureAccessor.put(name, accessName);
				b.closureVariable.put(name, type);
				subList.get(0).scopes.get(0).savedClosuresLocals.put(name, localName);
			}
			return false;
		}
		
	}
	
	public String localToField(String name) {
		return ctxStack.get(0).closureAccessor.get(name);
	}

	private String localToField(String name, FunctionBlock functionBlock) {
		return functionBlock.closureAccessor.get(name);
	}

	private int localToAuto(String name, FunctionBlock functionBlock) {
		for (ContextBlock cb : functionBlock.scopes){
			if (cb.type.containsKey(name))
				return cb.autoNumber.get(name);
		}
		throw new CompilationException("variable " + name + " unbound");
	}

	private boolean isLocalScope(String name, List<ContextBlock> scopes) {
		for (ContextBlock cb : scopes){
			if (cb.type.containsKey(name))
				return true;
		}
		return false;
	}

	public TypeRepresentation variableToType(String varName){
		for (FunctionBlock fb : ctxStack){
			TypeRepresentation tr = variableToType(varName, fb);
			if (tr != null)
				return tr;
		}
		return null;
	}
	
	private TypeRepresentation variableToType(String varName, FunctionBlock fb) {
		if (fb.closureVariable.containsKey(varName))
			return fb.closureVariable.get(varName);
		for (ContextBlock cb : fb.scopes){
			TypeRepresentation tr = cb.type.get(varName);
			if (tr != null)
				return tr;
		}
		return null;
	}

	/**
	 * Returns order id of local variable
	 * @param auto
	 * @return
	 */
	public Integer autoToNum(String varName){
		for (ContextBlock cb : ctxStack.get(0).scopes){
			Integer i = cb.autoNumber.get(varName);
			if (i != null)
				return i;
		}
		return null;
	}
	
	public TypeRepresentation getReturnType(){
		return ctxStack.get(0).returnType;
	}
	
	public TypeRepresentation getContextType(){
		return ctxStack.get(0).contextType;
	}
	
	public ContextBlock popFunctionBlock(){
		ctxStack.remove(0);
		if (ctxStack.size() == 0)
			return null;
		return ctxStack.get(0).scopes.get(0);
	}

	/**
	 * Pops the local scope. Happens at the end of the scope life. Will add local definition to the classdef
	 * @param mv
	 * @param start
	 */
	public void pop(MethodVisitor mv, Label start, boolean reuseOrdering){
		FunctionBlock fb = ctxStack.get(0);
		ContextBlock cb = fb.scopes.get(0);
        fb.scopes.remove(0);
		
		Label end = new Label();
        mv.visitLabel(end);
        for (String varName : cb.type.keySet()){
        	Integer varId = cb.autoNumber.get(varName);
        	TypeRepresentation varType = cb.type.get(varName);
        	
        	mv.visitLocalVariable(varName, varType.toJVMTypeString(), null, start, end, varId);
        }
		
		if (reuseOrdering){
			// clean up used order indexes
			Integer min = null;
			for (Integer i : cb.autoNumber.values()){
				if (min == null)
					min = i;
				min = Math.min(i, min);
			}
			if (min != null)
				fb.ordering = min;
		}
	}

}
