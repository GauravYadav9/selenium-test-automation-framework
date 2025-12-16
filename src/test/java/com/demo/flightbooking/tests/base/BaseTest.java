package com.demo.flightbooking.tests.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.demo.flightbooking.utils.ConfigReader;
import com.demo.flightbooking.utils.DriverManager;
import com.demo.flightbooking.utils.ExtentManager;

/**
 * The base class for all test classes in the framework.
 * It handles the setup and teardown of essential components like WebDriver,
 * ExtentReports, and logging, ensuring a consistent test execution lifecycle.
 */
public class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);

    // Thread-safe report instance for parallel execution (each browser runs in
    // isolated thread)
    private static final ThreadLocal<ExtentReports> extentReports = new ThreadLocal<>();

    // Shared list of failure summaries (thread-safe)
    protected static final List<String> failureSummaries = Collections.synchronizedList(new ArrayList<>());

    /**
     * This method runs once before the entire test suite.
     * It sets up the ExtentReports instance and configures the report's appearance.
     */
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        Logger suiteLogger = LogManager.getLogger(this.getClass());

        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        suiteLogger.info("Logs directory ensured.");
        String suiteName = System.getProperty("test.suite", "default");
        File oldSummary = new File("reports/" + suiteName + "-failure-summary.txt");
        if (oldSummary.exists()) {
            oldSummary.delete();
            suiteLogger.info("Old failure summary deleted.");
        }
    }

    /**
     * Runs once per <test> tag in testng XML.
     * Creates a unique ExtentSparkReporter per browser/stage.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        if (extentReports.get() == null) {
            Logger classLogger = LogManager.getLogger(this.getClass());

            // Get browser from system property (set by Jenkins Maven command)
            String browser = System.getProperty("browser", "chrome").toUpperCase();

            // Determine suite and report directory (e.g., chrome or firefox)
            String reportDir = System.getProperty("report.dir", browser.toLowerCase()); // fallback to browser
            String suiteName = System.getProperty("test.suite", "default");

            String reportPath = "reports/" + reportDir + "/";
            new File(reportPath).mkdirs(); // Ensure folder exists

            // File name like regression-chrome-report.html
            String reportFileName = suiteName + "-" + reportDir + "-report.html";

            // Create and configure ExtentSparkReporter
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath + reportFileName);
            sparkReporter.config().setOfflineMode(true);
            sparkReporter.config()
                    .setDocumentTitle("Test Report: " + suiteName.toUpperCase() + " - " + reportDir.toUpperCase());

            // Create new ExtentReports and attach reporter
            ExtentReports reports = new ExtentReports();
            reports.attachReporter(sparkReporter);
            reports.setSystemInfo("Tester", ConfigReader.getProperty("tester.name"));
            reports.setSystemInfo("OS", System.getProperty("os.name"));
            reports.setSystemInfo("Java Version", System.getProperty("java.version"));
            extentReports.set(reports);

            classLogger.info("Report will be generated at: {}{}", reportPath, reportFileName);
        }
    }

    /**
     * This method runs before each test method.
     * It initializes the WebDriver instance for the current thread and creates a
     * new
     * test entry in the ExtentReport.
     * 
     * @param method The test method that is about to be run.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        // ROBUST MDC: Set context for THIS test method's thread
        String mdcSuite = System.getProperty("test.suite", "unknown");

        // Get browser from system property (set by Jenkins Maven command)
        String browser = System.getProperty("browser", "chrome").toUpperCase();
        String mdcBrowser = (browser != null && !browser.isBlank()) ? browser : "UNKNOWN";

        // Set thread name for log4j %X{thread} pickup
        String customThreadName = "TestNG-test-" + mdcSuite.toLowerCase() + "-" + browser.toLowerCase() + "-"
                + Thread.currentThread().threadId();
        Thread.currentThread().setName(customThreadName);

        // Set MDC context for THIS thread
        ThreadContext.put("suite", mdcSuite.toUpperCase());
        ThreadContext.put("browser", mdcBrowser);
        ThreadContext.put("testname", method.getName());

        DriverManager.setBrowser(browser.toLowerCase());

        Logger methodLogger = LogManager.getLogger(this.getClass());
        methodLogger.info("Browser set to: {} for test: {}", browser, method.getName());

        String browserName = DriverManager.getBrowser().toUpperCase();
        // Create a test entry in report
        ExtentTest test = extentReports.get().createTest(method.getName() + " - " + browserName);
        ExtentManager.setTest(test);
        methodLogger.info("ExtentTest created for test: {} on {}", method.getName(), browserName);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Use logger after MDC is still set (cleared at the end)
        Logger methodLogger = LogManager.getLogger(this.getClass());

        // The TestListener is now 100% responsible for all report logging.
        // This method is ONLY for cleanup and failure summaries for Jenkins.

        // Add failure to summary for Jenkins email/dashboard (TestListener handles
        // ExtentReports)
        if (result.getStatus() == ITestResult.FAILURE) {
            String failureMsg = result.getMethod().getMethodName()
                    + " FAILED: " + result.getThrowable().getMessage().split("\n")[0];
            failureSummaries.add(failureMsg);

            methodLogger.error("Test failed: {}", result.getMethod().getMethodName());
        }

        DriverManager.quitDriver();
        methodLogger.info("WebDriver quit after test: {}", result.getMethod().getMethodName());
        ExtentManager.unload();

        ThreadContext.clearAll();
    }

    /**
     * Runs once per <test> tag completion.
     * Flushes report and writes failure summary.
     */
    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        Logger classLogger = LogManager.getLogger(this.getClass());

        if (extentReports.get() != null) {
            extentReports.get().flush();
            classLogger.info("ExtentReports flushed to disk.");
        }

        // Write failure summary for reporting/CI visibility

        String suiteName = System.getProperty("test.suite", "default");
        String mergedSummaryFile = "reports/" + suiteName + "-failure-summary.txt";

        if (!failureSummaries.isEmpty()) {
            try {
                File file = new File(mergedSummaryFile);
                boolean created = file.getParentFile().mkdirs();
                if (created) {
                    classLogger.info("Reports directory created.");
                }
                try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) { // append mode
                    failureSummaries.forEach(out::println);
                    classLogger.info("Failure summary appended to merged file: {}", mergedSummaryFile);
                }
            } catch (IOException e) {
                classLogger.error("Failed to write to merged failure summary", e);
            }
        }
    }
}
