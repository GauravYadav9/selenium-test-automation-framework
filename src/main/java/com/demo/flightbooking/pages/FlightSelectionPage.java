package com.demo.flightbooking.pages;

import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import com.demo.flightbooking.utils.WebDriverUtils;

/**
 * Represents the Flight Selection Page of the flight booking application.
 * This page appears after a user has searched for flights, and it lists
 * the available options.
 */
public class FlightSelectionPage extends BasePage {

    // WebDriverUtils instance for robust interactions
    private final WebDriverUtils webDriverUtils;

    // --- Locators ---
    private final By chooseFlightButton = By.cssSelector("input[type='submit']");
    // Locator for all the rows in the flight table
    private final By flightRows = By.xpath("//table[@class='table']/tbody/tr");
    // Locator for the price cell within a flight row (it's the 6th column: <td>)
    private final By priceCell = By.xpath("./td[6]");

    /**
     * Constructor for the FlightSelectionPage.
     *
     * @param driver The WebDriver instance.
     */
    public FlightSelectionPage(WebDriver driver) {
        super(driver);
        this.webDriverUtils = new WebDriverUtils(driver, resolveTimeout());
        logger.info("FlightSelectionPage initialized.");
    }

    // --- Action Methods ---

    /**
     * Verifies that the Flight Selection page is displayed by checking
     * if the flight table is visible.
     *
     * @return true if the flight table is displayed.
     */
    public boolean isFlightSelectionPageDisplayed() {
        boolean isDisplayed = webDriverUtils.isElementDisplayed(flightRows);
        logger.info("Flight Selection page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Clicks the "Choose This Flight" button to select the first available flight.
     */
    public void clickChooseFlightButton() {
        logger.info("Clicking Choose This Flight button.");
        webDriverUtils.waitForPageLoad();
        webDriverUtils.click(chooseFlightButton);
        logger.info("Choose Flight button clicked.");
    }

    /**
     * Returns the lowest flight price from the results table.
     *
     * @return An Optional<Double> containing the lowest price, or an empty Optional
     *         if no prices are found.
     */
    public Optional<Double> getLowestFlightPrice() {
        logger.info("Finding the lowest flight price on the page using Streams.");
        return driver.findElements(flightRows).stream()
                .map(row -> row.findElement(priceCell).getText())
                .map(priceText -> priceText.replace("$", ""))
                .map(Double::parseDouble)
                .min(Double::compare);
    }
}
