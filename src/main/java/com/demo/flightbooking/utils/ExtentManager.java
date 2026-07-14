package com.demo.flightbooking.utils;

import com.aventstack.extentreports.ExtentTest;

/**
 * Thread-safe manager for ExtentTest instances during parallel execution.
 * Associates test logs and results with their corresponding thread-local report entries.
 */
public class ExtentManager {

    /**
     * A ThreadLocal variable to store the ExtentTest instance for each thread.
     */
	private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    /**
     * Retrieves the ExtentTest instance for the current thread.
     *
     * @return The ExtentTest instance.
     */
    public static ExtentTest getTest() {
        return extentTest.get();
    }

    /**
     * Sets the ExtentTest instance for the current thread.
     * Called by TestListener during test initialization.
     *
     * @param test The ExtentTest instance to associate with the current thread.
     */
    public static void setTest(ExtentTest test) {
        extentTest.set(test);
    }

    /**
     * Removes the ExtentTest instance for the current thread.
     */
    public static void unload() {
        extentTest.remove();
    }
}