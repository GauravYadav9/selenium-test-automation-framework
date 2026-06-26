package com.demo.flightbooking.factory;

import com.demo.flightbooking.enums.BrowserType;
import com.demo.flightbooking.utils.ConfigReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Configures browser-specific capabilities.
 * Relies on native Selenium Manager (Selenium 4.6+) for local binary
 * resolution.
 * In Grid mode, driver management is handled by Grid nodes.
 */

public class BrowserOptionsFactory {

    private static final Logger logger = LogManager.getLogger(BrowserOptionsFactory.class);

    private BrowserOptionsFactory() {
        // Utility class — prevent instantiation
    }

    /**
     * Creates browser-specific options based on browser type and headless flag.
     * In local mode, relies on native Selenium Manager for binary resolution.
     * In Grid mode, skips binary resolution — Grid nodes manage their own drivers.
     * Supports dynamic browser version targeting via {@code -Dbrowser.version}.
     *
     * @param browserType The type of browser (e.g., CHROME, FIREFOX).
     * @param isHeadless  Whether the browser should run in headless mode.
     * @return MutableCapabilities configured for the specified browser.
     */

    public static MutableCapabilities getOptions(BrowserType browserType, boolean isHeadless) {

        logger.info("Configuring capabilities for {} (Headless: {})", browserType, isHeadless);
        String browserVersion = ConfigReader.getProperty("browser.version");
        if (browserVersion != null && !browserVersion.isEmpty()) {
            logger.info("Targeting specific {} version: {}", browserType, browserVersion);
        }

        switch (browserType) {
            case CHROME:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized", "--disable-gpu", "--remote-allow-origins=*");
                if (isHeadless) {
                    chromeOptions.addArguments("--headless=new", "--window-size=1920,1080");
                }
                if (browserVersion != null && !browserVersion.isEmpty()) {
                    chromeOptions.setBrowserVersion(browserVersion);
                }
                return chromeOptions;

            case FIREFOX:
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (isHeadless) {
                    firefoxOptions.addArguments("--headless", "--width=1920", "--height=1080");
                }
                if (browserVersion != null && !browserVersion.isEmpty()) {
                    firefoxOptions.setBrowserVersion(browserVersion);
                }
                return firefoxOptions;

            case EDGE:
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                if (isHeadless) {
                    edgeOptions.addArguments("--headless=new", "--window-size=1920,1080");
                }
                if (browserVersion != null && !browserVersion.isEmpty()) {
                    edgeOptions.setBrowserVersion(browserVersion);
                }
                return edgeOptions;

            default:
                throw new IllegalArgumentException("Unsupported browser type provided: " + browserType);
        }
    }
}