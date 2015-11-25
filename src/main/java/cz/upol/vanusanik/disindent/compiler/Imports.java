package cz.upol.vanusanik.disindent.compiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.FunctionSignatures;
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

	
	/**
	 * Adds din name to slash path resolve
	 * @param slashPath
	 * @param dinName
	 * @param originalPath
	 */
	public void add(String dinName, String originalPath){
		importMapOriginal.put(dinName, originalPath);
	}

	public void add(String packageDeclaration, String moduleName, String typedef, String searchDef){
		add(searchDef, (packageDeclaration.equals("") ? "" : packageDeclaration + ".") + moduleName + "." + typedef);
	}
	
	/**
	 * Tries to parse import string into actual imports
	 * @param fullImport import string
	 */
	public void addImport(String fullImport, boolean system){
		String[] components = Utils.splitByLastDot(fullImport);
		addImport(components[0], components[1], system);
	}

	/**
	 * Adds the unknown element to either functions of typedefs
	 * @param parentPath
	 * @param object
	 */
	public void addImport(String parentPath, String object, boolean system) {
		String[] components = Utils.splitByLastDot(parentPath);
		String packagePath = components[0];
		String moduleName = components[1];
		
		if (StringUtils.isAllUpperCase(object.substring(0, 1))){
			// module def, load its functions as Module.function and typedefs as Module.typedef
			FunctionSignatures fcs = BuildPath.getBuildPath().getSignatures(parentPath, object);
			for (String fncName : fcs.definedFunctions())
				add(parentPath, object, fncName, system ? fncName : object + "." + fncName);
			for (String typedef : BuildPath.getBuildPath().getTypedefs(parentPath, object))
				add(parentPath, object, typedef, system ? typedef : object + "." + typedef);
			return;
		}

		if (StringUtils.isAllLowerCase(moduleName.subSequence(0, 1)))
			throw new MalformedImportDeclarationException("package path does not end with a module declaration");
		
		add(packagePath, moduleName, object, object);
	}

	public void remove(String x) {
		importMapOriginal.remove(x);
	}
}
