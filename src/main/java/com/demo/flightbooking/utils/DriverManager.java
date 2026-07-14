package com.demo.flightbooking.utils;

import com.demo.flightbooking.enums.BrowserType;

import com.demo.flightbooking.factory.BrowserOptionsFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

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
 * Thread-safe WebDriver instance for parallel execution.
 *
 * Browser selection is resolved through ConfigReader
 * (System Property → configuration file → default).
 *
 * Browser isolation in CI is per-JVM (separate Maven processes per browser),
 * not per-thread.
 */
public class DriverManager {

    private DriverManager() {
        // Utility class — prevent instantiation
    }

    private static final Logger logger = LogManager.getLogger(DriverManager.class);

    /**
     * Thread-isolated WebDriver instance for parallel execution.
     */
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    /**
     * Returns true if the current thread already owns a WebDriver.
     * Unlike getDriver(), this method never creates a new browser session.
     */
    public static boolean hasDriver() {
        return driver.get() != null;
    }

    /**
     * Returns the existing WebDriver for the current thread without
     * triggering lazy initialization.
     *
     * Intended for teardown/reporting code where creating a new browser
     * session would be incorrect.
     */
    public static WebDriver getExistingDriver() {
        return driver.get();
    }

    /**
     * Retrieves the WebDriver instance for the current thread.
     * If an instance does not exist, it creates a new one based on the
     * configuration resolved through ConfigReader (e.g., browser type, grid
     * enabled, headless mode).
     *
     * @return The WebDriver instance for the current thread.
     */
    public static WebDriver getDriver() {
        if (driver.get() == null) {
            // ConfigReader handles the resolution: -Dbrowser=x → config.properties → "chrome"
            String browser = ConfigReader.getProperty("browser", "chrome");

            BrowserType browserType = BrowserType.valueOf(browser.toUpperCase(Locale.ENGLISH));
            boolean isHeadless = Boolean.parseBoolean(ConfigReader.getProperty("browser.headless", "true"));
            logger.info("Headless mode enabled? {}", isHeadless);
            boolean useGrid = Boolean.parseBoolean(ConfigReader.getProperty("selenium.grid.enabled", "true"));
            logger.info("Grid enabled? {}", useGrid);
            logger.info("Execution mode: {}", useGrid ? "REMOTE (Grid)" : "LOCAL");
            logger.info("Initializing {} driver for thread: {}", browserType, Thread.currentThread().threadId());
            MutableCapabilities options = BrowserOptionsFactory.getOptions(browserType, isHeadless);

            if (useGrid) {
                try {
                    String hubHost = ConfigReader.getProperty("selenium.hubHost", "selenium-hub");
                    String urlFormat = ConfigReader.getProperty("seleniumhub.urlFormat");

                    if (hubHost == null || hubHost.isEmpty()) {
                        throw new IllegalStateException("Missing hubHost or urlFormat in config.properties");
                    }
                    if (urlFormat == null || urlFormat.isEmpty()) {
                        throw new IllegalStateException(
                                "seleniumhub.urlFormat property is missing in config.properties");
                    }

                    String fullUrl = String.format(urlFormat, hubHost);
                    logger.info("Connecting to Selenium Grid at: {}", fullUrl);

                    URL gridUrl = URI.create(fullUrl).toURL();

                    driver.set(new RemoteWebDriver(gridUrl, options));
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Invalid Selenium Grid URL", e);
                }
            } else {
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

            if (!isHeadless) {
                driver.get().manage().window().maximize();
            }
        }

        return driver.get();
    }

    /**
     * Quits the WebDriver instance and removes the ThreadLocal reference.
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
        }
    }
}
