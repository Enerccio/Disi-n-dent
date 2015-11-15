package cz.upol.vanusanik.disindent.compiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.errors.MalformedImportDeclarationException;
import cz.upol.vanusanik.disindent.utils.Utils;

/**
 * Currently used imports. Used during compilation to resolve 
 * @author Peter Vanusanik
 *
 */
public class Imports implements Serializable {
	private static final long serialVersionUID = -453373772681489736L;

	/** Stores original names of the paths */
	Map<String, String> importMapOriginal
		= new HashMap<String, String>();
	/** Stores import paths of typedefs */
	Map<String, String> importMap
		= new HashMap<String, String>();
	/** Stores import paths of functions */
	Map<String, Set<String>> importMapFuncs
		= new HashMap<String, Set<String>>();

	
	/**
	 * Adds din name to slash path resolve
	 * @param slashPath
	 * @param dinName
	 * @param originalPath
	 */
	public void add(String slashPath, String dinName, String originalPath){
		importMap.put(dinName, slashPath);
		importMapOriginal.put(dinName, originalPath);
	}
	
	/**
	 * Adds typedef in package as import
	 * @param packageDeclaration
	 * @param moduleName
	 * @param typedef
	 */
	public void addTypedef(String packageDeclaration, String moduleName, String typedef){
		String fqName = (packageDeclaration.equals("") ? "" : Utils.slashify(packageDeclaration) + "/") + Utils.asModuledefJavaName(moduleName) + "$" + Utils.asTypedefJavaName(typedef);
		add(fqName, typedef, (packageDeclaration.equals("") ? "" : packageDeclaration + ".") + moduleName + "." + typedef);
	}
	
	/**
	 * Adds funcdef to the module path
	 * @param fqModulePath in . notation
	 * @param funcName
	 */
	public void addFuncdef(String fqModulePath, String funcName){
		String slashModulePath = Utils.slashify(fqModulePath);
		
		if (!importMapFuncs.containsKey(slashModulePath)){
			importMapFuncs.put(slashModulePath, new HashSet<String>());
		}
		importMapFuncs.get(slashModulePath).add(funcName);
		importMapOriginal.put(funcName, fqModulePath + "." + funcName);
	}
	
	/**
	 * Tries to parse import string into actual imports
	 * @param fullImport import string
	 */
	public void addImport(String fullImport){
		String[] components = Utils.splitByLastDot(fullImport);
		addImport(components[0], components[1]);
	}

	/**
	 * Adds the unknown element to either functions of typedefs
	 * @param parentPath
	 * @param object
	 */
	public void addImport(String parentPath, String object) {
		String[] components = Utils.splitByLastDot(parentPath);
		String packagePath = components[0];
		String moduleName = components[1];

		if (StringUtils.isAllLowerCase(moduleName.subSequence(0, 1)))
			throw new MalformedImportDeclarationException("package path does not end with a module declaration");
		
		String testTypedefName = (packagePath.equals("") ? "" : Utils.slashify(packagePath) + "/") + Utils.asModuledefJavaName(moduleName) + "$" + Utils.asTypedefJavaName(object);
		BuildPath bp = BuildPath.getBuildPath();
		
		if (bp.getClassSource(testTypedefName) == null){
			// it can only be function
			addFuncdef(packagePath + "." + moduleName, object);
		} else {
			// it is module
			addTypedef(packagePath, moduleName, object);
		}
	}
}
