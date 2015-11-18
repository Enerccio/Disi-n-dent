package cz.upol.vanusanik.disindent.runtime.node;

import java.io.File;

import com.beust.jcommander.Parameter;

/**
 * Options this console server application accepts.
 * 
 * @author Peter Vanusanik
 *
 */
public class NodeOptions {

	@Parameter(required = true, names = { "-p", "--port" }, description = "Port on which this node will listen")
	public Integer portNumber;
	
	@Parameter(required = false, names = { "-s", "--use-ssl" }, description = "Whether or not to use SSL")
	public boolean useSSL;

	@Parameter(required = false, names = { "-ks", "--keystore" }, description = "Keystore for SSL conecction.  Must be in same dir as running process.")
	public String keystore;

	@Parameter(required = false, names = { "-ksp", "--keystore-password" }, description = "Keystore password for SSL conecction.")
	public String keystorepass;

	@Parameter(names = { "-tc", "--thread-count" }, description = "Number of available node threads, default is number of cpu hardware threads available")
	public Integer threadCount = Runtime.getRuntime().availableProcessors();

	@Parameter(names = { "-cst", "--cache-store-duration" }, description = "Amount of time the cache is kept. Default 3600000 (in ms)")
	public Long cacheStoreTime = 3600L;

	@Parameter(names = { "-n", "--nodes" }, description = "List of nodes in <address>:<port>; format in single \"\" string")
	public String nodes = "";

	@Parameter(names = { "-nl", "--node-list" }, description = "File containing node list")
	public File nodeListFile;
	
	@Parameter(names = { "-t", "--timeout" }, description = "Maximum node execution timeout before termination in ms, default -1")
	public int timeout;
}
