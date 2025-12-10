package com.demo.flightbooking.factory;

import com.demo.flightbooking.enums.BrowserType;
import com.demo.flightbooking.utils.ConfigReader;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Configures browser-specific capabilities.
 * In local mode, delegates driver binary resolution to WebDriverManager;
 * in Grid mode, driver management is handled by Grid nodes.
 */

public class BrowserOptionsFactory {

    private static final Logger logger = LogManager.getLogger(BrowserOptionsFactory.class);

    private BrowserOptionsFactory() {
        // Utility class — prevent instantiation
    }

    /**
     * Creates browser-specific options based on browser type and headless flag.
     * In local mode, resolves driver binaries via WebDriverManager.
     * In Grid mode, skips binary resolution — Grid nodes manage their own drivers.
     *
     * @param browserType The type of browser (e.g., CHROME, FIREFOX).
     * @param isHeadless  Whether the browser should run in headless mode.
     * @return MutableCapabilities configured for the specified browser.
     */

    public static MutableCapabilities getOptions(BrowserType browserType, boolean isHeadless) {

        logger.info("Headless mode for {}: {}", browserType, isHeadless);

        boolean useGrid = Boolean.parseBoolean(ConfigReader.getProperty("selenium.grid.enabled", "true"));

        switch (browserType) {
            case CHROME:
                if (!useGrid) {
                    WebDriverManager.chromedriver().setup();
                }
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-gpu");
                chromeOptions.addArguments("--remote-allow-origins=*");

                if (isHeadless) {
                    logger.info("Enabling headless mode for CHROME");
                    chromeOptions.addArguments("--headless=new");
                    chromeOptions.addArguments("--window-size=1920,1080");
                }

                return chromeOptions;

            case FIREFOX:
                if (!useGrid) {
                    WebDriverManager.firefoxdriver().setup();
                }
                FirefoxOptions firefoxOptions = new FirefoxOptions();

                if (isHeadless) {
                    logger.info("Enabling headless mode for FIREFOX");
                    firefoxOptions.addArguments("--headless");
                    firefoxOptions.addArguments("--width=1920");
                    firefoxOptions.addArguments("--height=1080");
                }
                return firefoxOptions;

            case EDGE:
                if (!useGrid) {
                    WebDriverManager.edgedriver().setup();
                }
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");

                if (isHeadless) {
                    logger.info("Enabling headless mode for EDGE");
                    edgeOptions.addArguments("--headless=new");
                    edgeOptions.addArguments("--window-size=1920,1080");
                }

                return edgeOptions;

            default:
                throw new IllegalArgumentException("Unsupported browser type provided: " + browserType);
        }
    }
}