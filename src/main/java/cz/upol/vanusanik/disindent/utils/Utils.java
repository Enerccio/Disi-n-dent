package cz.upol.vanusanik.disindent.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Static utility class referenced in project containing helper methods
 * @author Peter Vanusanik
 *
 */
public class Utils {

	/**
	 * Combines class name and package name in correct way
	 * @param className
	 * @param packageName
	 * @return
	 */
	public static String fullNameForClass(String className, String packageName) {
		return packageName.equals("") ? className : packageName + "/" + className;
	}

	/**
	 * Wraps module name in L;
	 * @param moduleName
	 * @return LmoduleName;
	 */
	public static String asLName(String moduleName) {
		return "L" + moduleName + ";";
	}

	/**
	 * Converts . into /
	 * @param className
	 * @return / version of className
	 */
	public static String slashify(String className) {
		return StringUtils.replace(className, ".", "/");
	}
	
	/**
	 * Converts / into .
	 * @param slashName
	 * @return className version of slashName
	 */
	public static String deslashify(String slashName){
		return StringUtils.replace(slashName, "/", ".");
	}

}
