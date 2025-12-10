package com.demo.flightbooking.utils;

import com.aventstack.extentreports.ExtentTest;

/**
 * Manages the ExtentTest object in a thread-safe manner for parallel test execution.
 * Similar to DriverManager, this class uses a ThreadLocal variable to ensure
 * that logs and results from parallel tests are correctly written to their
 * corresponding test entries in the HTML report.
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
     * This is typically called in the @BeforeMethod of the BaseTest.
     *
     * @param test The ExtentTest instance to be associated with the current thread.
     */
    public static void setTest(ExtentTest test) {
        extentTest.set(test);
    }

    /**
     * Removes the ExtentTest instance for the current thread.
     * This is good practice for cleanup, though not as critical as quitting the driver.
     */
    public static void unload() {
        extentTest.remove();
    }
}