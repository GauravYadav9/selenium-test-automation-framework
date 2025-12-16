package com.demo.flightbooking.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.ITestNGMethod;

import com.demo.flightbooking.utils.ConfigReader;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ElementNotInteractableException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Professional grade retry analyzer for TestNG that intelligently retries only
 * infrastructure/timing issues while avoiding retries for genuine application
 * bugs.
 *
 * Features:
 * - Type-safe exception checking with inheritance support
 * - Cause chain traversal for wrapped exceptions
 * - Per-invocation retry tracking (handles DataProvider scenarios)
 * - Thread-safe implementation
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRY_COUNT = ConfigReader.getPropertyAsInt("test.retry.maxcount");

    // Track retries per-method-invocation (handles DataProvider and parallelism)
    private static final Map<String, Integer> retryCounts = new ConcurrentHashMap<>();

    // Infrastructure/timing exceptions that benefit from retry
    // RULE: Only include exceptions where "running again can fix the problem"
    private static final Set<Class<? extends Throwable>> RETRYABLE_EXCEPTIONS = new HashSet<>(
            Arrays.asList(
                    StaleElementReferenceException.class, // DOM changes during execution
                    ElementClickInterceptedException.class, // UI element blocking
                    TimeoutException.class, // Network/timing issues
                    NoSuchElementException.class, // Element loading timing
                    ElementNotInteractableException.class // UI state timing
            ));

    // Application/logic exceptions and Session-level failures that should NOT be retried
    // RULE: Session/browser is dead OR it's a code bug — retry cannot help
    private static final Set<Class<? extends Throwable>> NON_RETRYABLE_EXCEPTIONS = new HashSet<>(
            Arrays.asList(
                    // Code/Logic issues
                    AssertionError.class, // Test logic failures
                    IllegalArgumentException.class, // Code issues
                    NullPointerException.class, // Programming errors
                    ClassCastException.class, // Type issues
                    NoSuchMethodException.class, // Missing methods
                    IndexOutOfBoundsException.class, // Data issues
                    IllegalStateException.class, // State issues
                    // Session/Browser killers — retry cannot help
                    org.openqa.selenium.NoSuchSessionException.class, // Browser is dead
                    org.openqa.selenium.SessionNotCreatedException.class, // Browser couldn't start
                    org.openqa.selenium.NoSuchWindowException.class // Window is closed
            ));

    @Override
    public boolean retry(ITestResult result) {
        String invocationKey = getInvocationKey(result);
        int currentRetryCount = retryCounts.getOrDefault(invocationKey, 0);

        // Get the throwable object first for defensive programming
        Throwable throwable = result.getThrowable();

        // Check for max retries with null-safe logging
        if (currentRetryCount >= MAX_RETRY_COUNT) {
            logFailure(result.getMethod().getMethodName(),
                    (throwable != null) ? throwable.getClass().getSimpleName() : "N/A",
                    "max retries reached");
            return false;
        }

        // The null check remains as a safeguard
        if (throwable == null) {
            return false; // No exception, don't retry
        }

        String testMethodName = result.getMethod().getMethodName();

        // Check if this is a non-retryable application failure
        if (isNonRetryable(throwable)) {
            logDecision(testMethodName, throwable.getClass().getSimpleName(),
                    currentRetryCount, "non-retryable application failure");
            return false;
        }

        // Check if this is a retryable infrastructure issue
        if (isRetryable(throwable)) {
            int newRetryCount = currentRetryCount + 1;
            retryCounts.put(invocationKey, newRetryCount);
            logDecision(testMethodName, throwable.getClass().getSimpleName(),
                    newRetryCount, "retryable infrastructure issue");
            return true;
        }

        // Unknown exception - retry once to be safe
        if (currentRetryCount == 0) {
            retryCounts.put(invocationKey, 1);
            logDecision(testMethodName, throwable.getClass().getSimpleName(),
                    1, "unknown exception (retrying once)");
            return true;
        }

        logFailure(testMethodName, throwable.getClass().getSimpleName(),
                "unknown exception and max retries reached");
        return false;
    }

    /**
     * Checks if the exception (or its cause chain) is retryable
     */
    private boolean isRetryable(Throwable throwable) {
        return matchesExceptionType(throwable, RETRYABLE_EXCEPTIONS);
    }

    /**
     * Checks if the exception (or its cause chain) is non-retryable
     */
    private boolean isNonRetryable(Throwable throwable) {
        return matchesExceptionType(throwable, NON_RETRYABLE_EXCEPTIONS);
    }

    /**
     * Traverses the exception cause chain to find matches
     */
    private boolean matchesExceptionType(Throwable throwable, Set<Class<? extends Throwable>> exceptionTypes) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                if (exceptionType.isAssignableFrom(current.getClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates a unique key for each test method invocation (handles DataProvider
     * scenarios)
     */
    private String getInvocationKey(ITestResult result) {
        ITestNGMethod method = result.getMethod();
        String className = method.getRealClass().getName();
        String methodName = method.getMethodName();
        String parameters = Arrays.toString(result.getParameters());
        return className + "#" + methodName + parameters;
    }

    /**
     * Professional logging for retry decisions
     */
    private void logDecision(String testMethod, String exceptionType, int attemptCount, String reason) {
        String status = attemptCount > 0 ? "RETRY" : "SKIP";
        logger.info("[{}] {} - {} ({}, attempt {}/{})",
                testMethod, reason, exceptionType, status, attemptCount + 1, MAX_RETRY_COUNT + 1);
    }

    /**
     * Professional logging for final failure decisions
     */
    private void logFailure(String testMethod, String exceptionType, String reason) {
        logger.error("[{}] Not retrying - {}: {}", testMethod, reason, exceptionType);
    }
}