package com.demo.flightbooking.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import com.demo.flightbooking.utils.WebDriverUtils;

/**
 * Represents the Confirmation Page of the flight booking application.
 * This page is displayed after a successful flight booking,
 * showing the confirmation ID and total amount paid.
 */

public class ConfirmationPage extends BasePage {

    // WebDriverUtils for robust element interactions
    private final WebDriverUtils webDriverUtils;

    // ---Locators---
    private final By thankYouMessage = By.tagName("h1");
    private final By confirmationIdCell = By.xpath("//table//tr[1]/td[2]");
    private final By amountCell = By.xpath("//table//tr[3]/td[2]");

    /**
     * Constructor for the ConfirmationPage.
     *
     * @param driver The WebDriver instance.
     */
    public ConfirmationPage(WebDriver driver) {
        super(driver);
        this.webDriverUtils = new WebDriverUtils(driver, resolveTimeout());
        logger.info("ConfirmationPage initialized.");
    }

    // ---Action Methods----

    /**
     * Verifies that the confirmation page is displayed by checking
     * if the Thank You message element is visible.
     *
     * @return true if the Thank You message is displayed.
     */
    public boolean isConfirmationPageDisplayed() {
        boolean isDisplayed = webDriverUtils.isElementDisplayed(thankYouMessage);
        logger.info("Confirmation page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Gets the Thank you message displayed at the top of the confirmation page.
     *
     * @return The Thank you message as a String.
     */
    public String getThankYouMessage() {
        logger.debug("Getting Thank You Message.");
        return webDriverUtils.findElement(thankYouMessage).getText();
    }

    /**
     * Gets the confirmation ID from the page.
     *
     * @return The confirmation ID as a String.
     */
    public String getConfirmationId() {
        return webDriverUtils.findElement(confirmationIdCell).getText();
    }

    /**
     * Gets the total amount paid from the page.
     *
     * @return The total amount as a String (e.g., "555 USD").
     */
    public String getTotalAmount() {
        return webDriverUtils.findElement(amountCell).getText();
    }
}
