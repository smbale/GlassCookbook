package com.ustwo.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * Secure logging. Disables debug level logging for release builds.
 * Debug level logging is turned off by default, call init to automatically enable debug logging
 * depending on the build type.
 *
 * @author mark@ustwo.co.uk
 */
public class Slog {

	/** Flag to allow the debug to be forced on for release builds. */
	private static final boolean FORCE_DEBUG = false;

    private static boolean sDebug = false;

    /**
     * Call to automatically turn debug level logging on or off depending on the build type.
     * @param context Application context.
     */
    public static final void init(Context context) {
        // Determine if debugging should be enabled for this session.
        sDebug = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) ==
            ApplicationInfo.FLAG_DEBUGGABLE || FORCE_DEBUG;
    }

    /**
     * Performs debug logging. Only prints for debug builds, not releases.
     * Log tag is generated from the stack trace.
     * @see android.util.Log
     * @param message Message content.
     */
    public static final void d(String message) {
    	if(!sDebug) {
    		return;
    	}

    	d(getCaller(), message);
    }

    /**
     * Performs debug logging. Only prints for debug builds, not releases.
     * @see android.util.Log
     * @param tag Message tag.
     * @param message Message content.
     */
    public static final void d(String tag, String message) {
    	if(sDebug) {
    		Log.d(tag, message);
    	}
    }

    /**
     * Performs error logging. This is occurs whether debug is set or not.
     * Log tag is generated from the stack trace.
     * @see android.util.Log
     * @param message Message content.
     */
    public static final void e(String message) {
    	e(getCaller(), message);
    }

    /**
     * Performs error logging. This is occurs whether debug is set or not.
     * Log tag is generated from the stack trace.
     * @see android.util.Log
     * @param message Message content.
     * @param throwable Throwable exception.
     */
    public static final void e(String message, Throwable throwable) {
        e(getCaller(), message, throwable);
    }

    /**
     * Performs error logging. This is occurs whether debug is set or not.
     * @see android.util.Log
     * @param tag Message tag.
     * @param message Message content.
     */
    public static final void e(String tag, String message) {
    	Log.e(tag, message);
    }

    /**
     * Performs error logging. This is occurs whether debug is set or not.
     * @see android.util.Log
     * @param tag Message tag.
     * @param message Message content.
     * @param throwable Throwable exception.
     */
    public static final void e(String tag, String message, Throwable throwable) {
    	Log.e(tag, message, throwable);
    }

    /**
     * Get the caller of the log method.
     * @return
     */
    private static String getCaller() {
        StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
        StringBuilder messageBuilder = new StringBuilder();

        /*
         * The first stack element comes from a native method, the second comes from
         * Thread.getStackTrace. The third stack element is this
         * function, ction, fourth , finally the fifth is the caller of log.
         *
         * Note that this differs from the behavior found in JavaSE.
         */
        try {
            StackTraceElement caller = currentStack[4];
            messageBuilder.append(caller.getFileName()).append("[").append(caller.getLineNumber())
                    .append("].").append(caller.getMethodName());
        } catch (IndexOutOfBoundsException e) {
            // In case there is something special about the stack..
        }

        return messageBuilder.toString();
    }

    /**
     * Check if Slog is enabled for this build.
     * @return true if it is, otherwise false.
     */
    public static boolean isEnabled() {
        return sDebug;
    }
}
