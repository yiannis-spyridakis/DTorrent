package com.torr.utils;


/**
 * System utilities
 *
 */
public class SystemUtils {
	
	// Returns a path to the user's appdata area in a cross platform way
	public static String GetDefaultDirectory()
	{
	    String OS = System.getProperty("os.name").toUpperCase();
	    if (OS.contains("WIN"))
	        return System.getenv("APPDATA");
	    else if (OS.contains("MAC"))
	        return System.getProperty("user.home") + "/Library/Application "
	                + "Support";
	    else if (OS.contains("NUX"))
	        return System.getProperty("user.home");
	    return System.getProperty("user.dir");
	}
}
