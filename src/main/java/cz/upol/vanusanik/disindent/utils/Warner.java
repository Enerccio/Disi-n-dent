package cz.upol.vanusanik.disindent.utils;

/**
 * Helper utility that warns or throws exception if set to do so
 * @author Peter Vanusanik
 *
 */
public class Warner {
	
	/**
	 * WarnLevel indicates what to do when warning happens
	 * @author Peter Vanusanik
	 *
	 */
	public enum WarnLevel {
		/** Warning is ignored completely */
		SILENT, 
		/** Warning is logged to stderr */
		WARN, 
		/** Warning is logged to srderr with full stacktrace */
		WARN_VERBOSE,
		/** Warning is thrown */
		ERROR
	}
	
	private static WarnLevel warnLevel = WarnLevel.WARN;
	
	/**
	 * Sets the warn level to the desired level.
	 * @param level
	 */
	public static void setWarnLevel(WarnLevel level){
		warnLevel = level;
	}
	
	/**
	 * Creates a warning. The exception will be RuntimeException.
	 * @param message warning message
	 */
	public static void warn(String message){
		warn(new RuntimeException(message));
	}
	
	/**
	 * Creates a warning. This exception will be potentially thrown and message will be created from e.getMessage()
	 * @param e exception to use as warning
	 */
	public static void warn(RuntimeException e){
		try {
			warn((Exception) e);
		} catch (Exception e1) {
			throw (RuntimeException)e1;
		}
	}

	/**
	 * Creates a warning. This exception will be potentially thrown and message will be created from e.getMessage()
	 * @param e exception to use as warning
	 */
	public static void warn(Exception e) throws Exception {
		if (warnLevel == WarnLevel.WARN || warnLevel == WarnLevel.WARN_VERBOSE){
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
		
		if (warnLevel == WarnLevel.WARN_VERBOSE)
			e.printStackTrace(System.err);
		
		if (warnLevel != WarnLevel.ERROR)
			return;
		
		throw e;
	}

	private Warner(){
		
	}
	
}
