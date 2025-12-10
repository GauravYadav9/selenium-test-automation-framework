package com.demo.flightbooking.utils;

import com.demo.flightbooking.enums.BrowserType;

import com.demo.flightbooking.factory.BrowserOptionsFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Manages the WebDriver instance in a thread-safe manner for parallel test
 * execution.
 * This class ensures that each test thread gets its own separate WebDriver
 * instance,
 * preventing conflicts and instability during parallel runs.
 */
public class DriverManager {

    private DriverManager() {
        // Utility class — prevent instantiation
    }

    private static final Logger logger = LogManager.getLogger(DriverManager.class);
    /**
     * A ThreadLocal variable to store the WebDriver instance.
     * This is the key to achieving thread safety in parallel execution.
     */
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final ThreadLocal<String> browserName = new ThreadLocal<>();

    public static void setBrowser(String browser) {
        logger.info("Setting browser for current thread to: {}", browser.toUpperCase());
        browserName.set(browser.toLowerCase());
    }

    public static String getBrowser() {
        return browserName.get();
    }

    /**
     * Retrieves the WebDriver instance for the current thread.
     * If an instance does not exist, it creates a new one based on the
     * configuration in config.properties (e.g., browser type, grid enabled,
     * headless mode).
     *
     * @return The WebDriver instance for the current thread.
     */
    public static WebDriver getDriver() {
        if (driver.get() == null) {
            String browser = browserName.get() != null
                    ? browserName.get()
                    : ConfigReader.getProperty("browser");

            BrowserType browserType = BrowserType.valueOf(browser.toUpperCase());
            boolean isHeadless = Boolean.parseBoolean(ConfigReader.getProperty("browser.headless", "true"));
            logger.info("Headless mode enabled? {}", isHeadless);
            boolean useGrid = Boolean.parseBoolean(ConfigReader.getProperty("selenium.grid.enabled", "true"));
            logger.info("Grid enabled? {}", useGrid);
            logger.info("Execution mode: {}", useGrid ? "REMOTE (Grid)" : "LOCAL");
            logger.info("Initializing {} driver for thread: {}", browserType, Thread.currentThread().threadId());
            MutableCapabilities options = BrowserOptionsFactory.getOptions(browserType, isHeadless);

            if (useGrid) {
                try {
                    // Validate required properties
                    String hubHost = ConfigReader.getProperty("selenium.hubHost", "selenium-hub");
                    String urlFormat = ConfigReader.getProperty("seleniumhub.urlFormat");

                    if (hubHost == null || hubHost.isEmpty()) {
                        throw new IllegalStateException("Missing hubHost or urlFormat in config.properties");
                    }
                    if (urlFormat == null || urlFormat.isEmpty()) {
                        throw new IllegalStateException("seleniumhub.urlFormat property is missing in config.properties");
                    }

                    String fullUrl = String.format(urlFormat, hubHost);
                    logger.info("Connecting to Selenium Grid at: {}", fullUrl);

                    URL gridUrl = URI.create(fullUrl).toURL();

                    driver.set(new RemoteWebDriver(gridUrl, options));
                } catch (MalformedURLException e) {
                    logger.error("Malformed Selenium Grid URL: {}", e.getMessage());
                    throw new IllegalStateException("Invalid Selenium Grid URL", e);
                }
            } else {
                // Local Mode
                switch (browserType) {
                    case CHROME:
                        driver.set(new ChromeDriver((ChromeOptions) options));
                        break;
                    case FIREFOX:
                        driver.set(new FirefoxDriver((FirefoxOptions) options));
                        break;
                    case EDGE:
                        driver.set(new EdgeDriver((EdgeOptions) options));
                        break;
                    default:
                        throw new IllegalStateException("Unsupported browser type: " + browserType);
                }
            }

            driver.get().manage().window().maximize();
        }

        return driver.get();
    }

    /**
     * Quits the WebDriver instance for the current thread and removes it from the
     * ThreadLocal variable.
     * This is crucial for cleaning up resources and preventing memory leaks after a
     * test is complete.
     */
    public static void quitDriver() {
        WebDriver wd = driver.get();
        try {
            if (wd != null) {
                logger.info("Quitting driver for thread: {}", Thread.currentThread().threadId());
                wd.quit();
            } else {
                logger.warn("quitDriver called but thread-local WebDriver was null.");
            }
        } finally {
            ThreadContext.remove("browser");
            driver.remove();
            browserName.remove();
        }
    }
}
