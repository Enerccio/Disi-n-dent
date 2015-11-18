package cz.upol.vanusanik.disindent.runtime.cli;

import java.io.File;

import com.beust.jcommander.Parameter;

/**
 * Options for CLI version of the program
 * 
 * @author Peter Vanusanik
 *
 */
public class DisindentCLIOptions {

	@Parameter(names = { "-sd", "--source-directory" }, description = "Directory where are source files that will be compiled. Default is cwd")
	public File sourcesDirectory = new File(System.getProperty("user.dir"));
	
	@Parameter(required = false, names = { "-s", "--use-ssl" }, description = "Whether or not to use SSL")
	public boolean useSSL;

	@Parameter(required = true, arity = 1, description = "fully qualified path to the function to be executed (ie com.example.Foobar.baz)")
	public String main = null;

	@Parameter(names = { "-ia", "--init-args" }, description = "You can specify number only parameters that will be applied to your starting function as arguments in this as string enclosed by \"\".")
	public String initialFuncArgs = "";

	@Parameter(names = { "-n", "--nodes" }, description = "List of nodes in <address>:<port>; format in single \"\" string")
	public String nodes = "";

	@Parameter(names = { "-nl", "--node-list" }, description = "File containing node list")
	public File nodeListFile;

	@Parameter(required = false, names = { "-ks", "--keystore" }, description = "Keystore for SSL conecction.  Must be in same dir as running process.")
	public String keystore;

	@Parameter(required = false, names = { "-ksp", "--keystore-password" }, description = "Keystore password for SSL conecction.")
	public String keystorepass;

}
