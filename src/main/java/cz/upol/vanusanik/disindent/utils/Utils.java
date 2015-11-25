package cz.upol.vanusanik.disindent.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.errors.CompilationException;

/**
 * Static utility class referenced in project containing helper methods
 * 
 * @author Peter Vanusanik
 *
 */
public class Utils {

	public static final CharSequence[] ASCII_LOWERCASE_LIST = new CharSequence[] {
			"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
			"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", };

	/**
	 * Combines class name and package name in correct way
	 * 
	 * @param className
	 * @param packageName
	 * @return
	 */
	public static String fullNameForClass(String className, String packageName) {
		return packageName.equals("") ? className : packageName + "/"
				+ className;
	}

	/**
	 * Wraps module name in L;
	 * 
	 * @param moduleName
	 * @return LmoduleName;
	 */
	public static String asLName(String moduleName) {
		return "L" + moduleName + ";";
	}

	/**
	 * Converts . into /
	 * 
	 * @param className
	 * @return / version of className
	 */
	public static String slashify(String className) {
		return StringUtils.replace(className, ".", "/");
	}

	/**
	 * Converts / into .
	 * 
	 * @param slashName
	 * @return className version of slashName
	 */
	public static String deslashify(String slashName) {
		return StringUtils.replace(slashName, "/", ".");
	}

	/**
	 * Converts typedef into java class name
	 * 
	 * @param typedef
	 * @return java class name
	 */
	public static String asTypedefJavaName(String typedef) {
		return StringUtils.capitalize(typedef);
	}

	/**
	 * Converts moduledef into java class name
	 * 
	 * @param moduledef
	 * @return java class name
	 */
	public static String asModuledefJavaName(String moduledef) {
		return "$dv$m$" + StringUtils.capitalize(moduledef);
	}

	/**
	 * Splits path by the last dot. "foo.bar.baz" => "foo.bar", "baz"
	 * 
	 * @param path
	 * @return
	 */
	public static String[] splitByLastDot(String path) {
		String[] res = new String[] { "", path };

		if (path.contains(".")) {
			res[0] = path.substring(0, path.lastIndexOf("."));
			res[1] = path.substring(path.lastIndexOf(".") + 1);
		}

		return res;
	}
	
	/**
	 * Splits path by the last slash. "foo/bar/baz" => "foo/bar", "baz"
	 * 
	 * @param path
	 * @return
	 */
	public static String[] splitByLastSlash(String path) {
		String[] res = new String[] { "", path };

		if (path.contains("/")) {
			res[0] = path.substring(0, path.lastIndexOf("/"));
			res[1] = path.substring(path.lastIndexOf("/") + 1);
		}

		return res;
	}

	/**
	 * Shallow search in parse tree for specified elements
	 * 
	 * @param clazz
	 * @param tree
	 * @return
	 */
	public static <T extends ParseTree> List<T> searchForElementOfType(
			Class<T> clazz, ParseTree tree) {
		return searchForElement(clazz, tree, false);
	}

	/**
	 * Deep or shallow search for specified elements in parser tree
	 * 
	 * @param clazz
	 * @param tree
	 * @param deep
	 *            whether it is deep or shallow search
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <T extends ParseTree> List<T> searchForElement(
			Class<T> clazz, ParseTree tree, boolean deep) {
		List<T> results = new ArrayList<T>();
		Queue<ParseTree> pq = new LinkedList<ParseTree>();
		pq.add(tree);

		while ((tree = pq.poll()) != null) {
			if (clazz.isInstance(tree)) {
				results.add((T) tree);
				if (!deep)
					continue;
			}
			for (int i = 0; i < tree.getChildCount(); i++)
				pq.add(tree.getChild(i));
		}

		return results;
	}

	/**
	 * Removes escapes and puts normal characters
	 * 
	 * @param strValue
	 * @return
	 */
	public static String removeEscapes(String strValue) {
		while (strValue.contains("\\")) {
			String leftPart = strValue.substring(0, strValue.indexOf('\\'));
			String rightPart = strValue.substring(strValue.indexOf('\\') + 1);
			char testee = strValue.charAt(strValue.indexOf('\\')+1);

			switch (testee) {
			case 'n':
				strValue = leftPart + "\n" + rightPart;
				continue;
			case '\'':
				strValue = leftPart + "'" + rightPart;
				continue;
			case 'r':
				strValue = leftPart + "\r" + rightPart;
				continue;
			case 't':
				strValue = leftPart + "\t" + rightPart;
				continue;
			case '\\':
				strValue = leftPart + "\\" + rightPart;
				continue;
			default:
				throw new CompilationException("unrecognized escape '"
						+ Character.toString(testee) + "'");
			}
		}
		return strValue;
	}

	/**
	 * Converts com.example.Module into com/example/_m$Module
	 * @param module
	 * @return
	 */
	public static String asJavaModuleName(String module) {
		String[] cs = splitByLastDot(module);
		return slashify((!cs[0].equals("") ? cs[0] + "." : "") + asModuledefJavaName(cs[1]));
	}

	public static boolean disindentClass(String name) {
		String[] split = StringUtils.split(name, "/");
		return split[split.length-1].startsWith("$dv$") || split[split.length-1].startsWith("$di$");
	}

	public static String asContextName(String ctxName) {
		return StringUtils.capitalize(ctxName) + "Context";
	}

	public static Object[] prepend(Object context, Object[] parameters) {
		ArrayList<Object> o = new ArrayList<Object>(Arrays.asList(parameters));
		o.add(0, context);
		return o.toArray();
	}
}
