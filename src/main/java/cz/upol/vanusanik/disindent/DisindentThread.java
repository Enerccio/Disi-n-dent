package cz.upol.vanusanik.disindent;

import java.io.File;

import cz.upol.vanusanik.disindent.buildpath.BuildPath;
import cz.upol.vanusanik.disindent.buildpath.CompilerOptions;

/**
 * Operations for enabling disindent for thread, mainly deals with build path, see also BuildPath
 * @author Peter Vanusanik
 *
 */
public class DisindentThread {

	/**
	 * Initializes Disindent for this thread
	 */
	public static void startInitialization(CompilerOptions compilerOptions){
		BuildPath bp = BuildPath.getBuildPath();
		bp.setGlobalOptions(compilerOptions);
	}
	
	/**
	 * Sets the BuildPath for this thread, old build path (if any) is discarded
	 * @param other new build path
	 */
	public static void setBuildPath(BuildPath other){
		BuildPath.setForThread(other);
	}
	
	/**
	 * Adds path to the build path for this thread
	 * @param path
	 */
	public static void addBuildPathPath(String path){
		BuildPath.getBuildPath().addPath(new File(path));
	}
	
	/**
	 * MUST BE CALLED BEFORE USING DISINDENT
	 * Finishes the initialization of Disindent for this thread
	 */
	public static void finalizeInitialization(){
		BuildPath.getBuildPath().validate();
	}
	
	/**
	 * Initializes Disindent for this thread 
	 * @param bp - list of paths used for Disindent build path
	 */
	public static void simpleStart(CompilerOptions compilerOptions, String... bp){
		startInitialization(compilerOptions);
		for (String path : bp)
			addBuildPathPath(path);
		finalizeInitialization();
	}
}
