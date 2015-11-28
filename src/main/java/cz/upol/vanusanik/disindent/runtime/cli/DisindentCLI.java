package cz.upol.vanusanik.disindent.runtime.cli;

import java.io.File;
import java.io.FileInputStream;

import com.beust.jcommander.JCommander;

import cz.upol.vanusanik.disindent.DisindentThread;
import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.CompilerOptions;
import cz.upol.vanusanik.disindent.runtime.network.NodeList;
import cz.upol.vanusanik.disindent.runtime.types.Method;
import cz.upol.vanusanik.disindent.utils.Utils;
import cz.upol.vanusanik.disindent.utils.Warner;
import cz.upol.vanusanik.disindent.utils.Warner.WarnLevel;

/**
 * Main class for CLI runtime
 * 
 * @author Peter Vanusanik
 *
 */
public class DisindentCLI {

	/**
	 * Entry point
	 * 
	 * @param args
	 * @throws Throwable 
	 */
	public static void main(String[] args) throws Throwable {
		DisindentCLIOptions no = new DisindentCLIOptions();
		new JCommander(no, args);

		Warner.setWarnLevel(WarnLevel.WARN_VERBOSE);
		run(no);
	}

	private static void run(DisindentCLIOptions no) throws Throwable {
		// Set up ssl/tsl truststores and keystores
		if (no.useSSL){
			System.setProperty("javax.net.ssl.keyStore", no.keystore);
			System.setProperty("javax.net.ssl.trustStore", no.keystore);
			System.setProperty("javax.net.ssl.keyStorePassword", no.keystorepass);
		}
		
		NodeList.setUseSSL(no.useSSL);

		File workingDir = no.sourcesDirectory;
		DisindentThread.simpleStart(CompilerOptions.compileInitialArgs(no.compilerArgs), workingDir.getAbsolutePath());

		String execFunc = no.main.get(0).replace("::", ".");

		// transforms arguments passed into cli into PLangObjects
		Object[] args = loadArgs(no.initialFuncArgs);

		// Process nodes from a file into nodelist
		if (no.nodeListFile != null) {
			FileInputStream fis = new FileInputStream(no.nodeListFile);
			NodeList.loadFile(fis);
			fis.close();
		}

		// Process nodes from cli arguments
		String[] parsedNodes = no.nodes.split(";");
		for (String s : parsedNodes) {
			if (!s.equals("")) {
				String[] datum = s.split(":");
				NodeList.addNode(datum[0], Integer.parseInt(datum[1]));
			}
		}

		String[] components = Utils.splitByLastDot(execFunc);
		String module = components[0];
		String func = components[1];
		// execute code
		Class<?> moduleClass = BuildPath.getBuildPath().getClassLoader().findClass(Utils.asJavaModuleName(module));
		Object mod = moduleClass.newInstance();
		Method execMethod = (Method) moduleClass.getField(func).get(mod);
		System.out.println(execMethod.invoke(args));
	}

	/**
	 * Transforms arguments in cli into java objects
	 * 
	 * @param args
	 * @return
	 */
	private static Object[] loadArgs(String args) {
		String[] initialFuncArgs = args.split(" ");
		Object[] array = new Object[initialFuncArgs.length];

		int iter = 0;
		for (String ia : initialFuncArgs) {
			try {
				int val = Integer.parseInt(ia);
				array[iter++] = Integer.valueOf(val);
			} catch (NumberFormatException ignores) {
				float val = Float.parseFloat(ia);
				array[iter++] = Float.valueOf(val);
			}
		}

		return array;
	}

}
