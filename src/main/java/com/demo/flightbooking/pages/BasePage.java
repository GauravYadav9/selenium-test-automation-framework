package com.demo.flightbooking.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import com.demo.flightbooking.utils.ConfigReader;

/**
 * Base class for all Page Objects in the framework.
 * It initializes the WebDriver and logger, and provides common
 * page functionalities that can be inherited by all specific page classes.
 */
public abstract class BasePage {

    protected WebDriver driver;
    protected final Logger logger;
    private static final int DEFAULT_TIMEOUT = 10;

    /**
     * Constructor for the BasePage.
     * Initializes the WebDriver and logger for the page.
     *
     * @param driver The WebDriver instance to be used by the page.
     */

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.logger = LogManager.getLogger(this.getClass());
    }

    /**
     * Resolves the test timeout from configuration with a defensive fallback.
     * Centralizes timeout resolution so Page Objects don't repeat this logic.
     *
     * @return timeout in seconds from config, or default if missing/invalid.
     */
    protected int resolveTimeout() {
        int timeout = ConfigReader.getPropertyAsInt("test.timeout");
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
            logger.warn("Invalid or missing 'test.timeout'. Falling back to {} seconds.", timeout);
        }
        return timeout;
    }

    /**
     * Gets the title of the current page.
     *
     * @return A string representing the page title.
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Get the current URL.
     *
     * @return The current URL.
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

}