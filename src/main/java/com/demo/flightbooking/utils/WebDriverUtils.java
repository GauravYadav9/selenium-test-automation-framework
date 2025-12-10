package com.demo.flightbooking.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Centralized WebDriver interaction utilities with explicit wait strategies.
 * All page objects delegate element interactions through this class for
 * consistent timeout behavior.
 */
public class WebDriverUtils {

    private static final Logger staticLogger = LogManager.getLogger(WebDriverUtils.class);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Logger logger;

    /**
     * Locator substrings that identify sensitive fields.
     * When sendKeys() targets one of these, the logged value is masked
     * while the actual browser input remains unmasked.
     */
    private static final Set<String> SENSITIVE_FIELD_IDENTIFIERS = Set.of(
            "creditcardnumber", "cardnumber", "cvv", "ssn", "password");

    /**
     * Constructor for WebDriverUtils.
     * Initializes the WebDriverWait with a timeout defined in the config file.
     *
     * @param driver The WebDriver instance.
     */
    public WebDriverUtils(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        this.logger = LogManager.getLogger(WebDriverUtils.class); // Logger for this utility class
    }

    /**
     * Checks if a locator targets a sensitive field by comparing its string
     * representation against known sensitive field identifiers.
     *
     * @param locator The By locator to check.
     * @return true if the locator matches a sensitive field identifier.
     */
    private boolean isSensitiveField(By locator) {
        String locatorStr = locator.toString().toLowerCase();
        return SENSITIVE_FIELD_IDENTIFIERS.stream().anyMatch(locatorStr::contains);
    }

    /**
     * Waits for an element to be present on the DOM and visible, then returns it.
     *
     * @param locator The By locator of the element.
     * @return The WebElement if found and visible.
     * @throws TimeoutException if the element is not found within the timeout
     *                          period.
     */
    public WebElement findElement(By locator) {
        logger.debug("Attempting to find element by: {}", locator);
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            logger.info("Found element: {}", locator);
            return element;
        } catch (TimeoutException e) {
            logger.error("Element not found or not visible within timeout: {}", locator, e);
            throw new NoSuchElementException("Element not found or not visible: " + locator, e);
        }
    }

    /**
     * Waits for all elements located by the given locator to be present on the DOM
     * and visible, then returns them.
     *
     * @param locator The By locator of the elements.
     * @return A list of WebElements.
     * @throws TimeoutException if no elements are found within the timeout period.
     */
    public List<WebElement> findElements(By locator) {
        logger.debug("Attempting to find elements by: {}", locator);
        try {
            List<WebElement> elements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
            logger.info("Found {} elements for locator: {}", elements.size(), locator);
            return elements;
        } catch (TimeoutException e) {
            logger.warn("No elements found or not visible within timeout for: {}", locator);
            return List.of(); // Return an empty list instead of throwing an exception if no elements are
                              // found
        }
    }

    /**
     * Clicks on a web element after waiting for it to be clickable.
     *
     * @param locator The By locator of the element to click.
     */
    public void click(By locator) {
        logger.info("Clicking element: {}", locator);
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            element.click();
            logger.info("Successfully clicked element: {}", locator);
        } catch (TimeoutException e) {
            logger.error("Element not clickable within timeout: {}", locator, e);
            throw e;
        } catch (WebDriverException e) {
            logger.error("Error clicking element {}: {}", locator, e.getMessage(), e);
            throw e; // Re-throw other WebDriver exceptions
        }
    }

    /**
     * Sends text to a web element after waiting for it to be visible.
     * Clears the field before sending keys.
     *
     * @param locator The By locator of the input field.
     * @param text    The text to send.
     */
    public void sendKeys(By locator, String text) {
        String logSafeText = isSensitiveField(locator) ? MaskingUtil.maskSensitiveValue(text) : text;
        logger.info("Sending keys '{}' to element: {}", logSafeText, locator);
        try {
            WebElement element = findElement(locator); // Uses findElement to ensure visibility
            element.clear();
            element.sendKeys(text); // Actual value sent to browser — NOT masked
            logger.info("Successfully sent keys '{}' to element: {}", logSafeText, locator);
        } catch (WebDriverException e) {
            logger.error("Error sending keys to element {}: {}", locator, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Selects an option from a dropdown by visible text.
     *
     * @param locator The By locator of the select element.
     * @param text    The visible text of the option to select.
     */
    public void selectByVisibleText(By locator, String text) {
        logger.info("Selecting '{}' from dropdown: {}", text, locator);
        try {
            WebElement selectElement = findElement(locator); // Uses findElement to ensure visibility
            Select select = new Select(selectElement);
            select.selectByVisibleText(text);
            logger.info("Successfully selected '{}' from dropdown: {}", text, locator);
        } catch (NoSuchElementException e) {
            logger.error("Option with text '{}' not found in dropdown {}.", text, locator, e);
            throw e;
        } catch (WebDriverException e) {
            logger.error("Error selecting from dropdown {}: {}", locator, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Gets the text of a web element after waiting for it to be visible.
     *
     * @param locator The By locator of the element.
     * @return The text of the element.
     */
    public String getText(By locator) {
        logger.debug("Getting text from element: {}", locator);
        WebElement element = findElement(locator); // Uses findElement to ensure visibility
        String text = element.getText();
        logger.info("Retrieved text '{}' from element: {}", text, locator);
        return text;
    }

    /**
     * Checks if an element is displayed on the page.
     *
     * @param locator The By locator of the element.
     * @return true if the element is displayed, false otherwise.
     */
    public boolean isElementDisplayed(By locator) {
        logger.debug("Checking if element is displayed: {}", locator);
        try {
            return findElement(locator).isDisplayed();
        } catch (NoSuchElementException | TimeoutException e) {
            logger.info("Element {} is not displayed.", locator);
            return false;
        } catch (StaleElementReferenceException e) {
            logger.warn("StaleElementReferenceException while checking display status for {}. Retrying...", locator);
            try {
                // Retry once
                return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
            } catch (Exception retryEx) {
                logger.error("Element {} is still not displayed after retry.", locator, retryEx);
                return false;
            }
        }
    }

    /**
     * Waits for the URL to contain a specific string.
     *
     * @param urlChunk The string expected to be in the URL.
     * @return true if the URL contains the string, false otherwise.
     */
    public boolean waitUntilUrlContains(String urlChunk) {
        logger.info("Waiting for URL to contain: {}", urlChunk);
        try {
            return wait.until(ExpectedConditions.urlContains(urlChunk));
        } catch (TimeoutException e) {
            logger.error("URL did not contain '{}' within timeout. Current URL: {}", urlChunk, driver.getCurrentUrl());
            return false;
        }
    }

    /**
     * Waits for the page title to contain a specific string.
     *
     * @param titleChunk The string expected to be in the page title.
     * @return true if the title contains the string, false otherwise.
     */
    public boolean waitUntilTitleContains(String titleChunk) {
        logger.info("Waiting for title to contain: {}", titleChunk);
        try {
            return wait.until(ExpectedConditions.titleContains(titleChunk));
        } catch (TimeoutException e) {
            logger.error("Page title did not contain '{}' within timeout. Current title: {}", titleChunk,
                    driver.getTitle());
            return false;
        }
    }

    public void waitForPageLoad() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState")));
    }

    public static void waitForPageLoad(WebDriver driver, Duration timeout) {
        new WebDriverWait(driver, timeout)
                .until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    public static void waitForAjaxComplete(WebDriver driver, Duration timeout) {
        new WebDriverWait(driver, timeout).until(d -> {
            Object js = ((JavascriptExecutor) d).executeScript(
                    "return (typeof jQuery === 'undefined') ? true : (jQuery.active === 0)");
            return Boolean.TRUE.equals(js);
        });
    }

    public static void click(WebDriver driver, By locator, Duration timeout) {
        WebElement el = new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            staticLogger.warn("Click intercepted for {}. Falling back to JS click. Reason: {}", locator, e.getMessage());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

}
