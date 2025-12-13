package com.demo.flightbooking.pages;

import com.demo.flightbooking.model.Passenger;
import com.demo.flightbooking.utils.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Represents the Purchase Page of the flight booking application.
 * This is the final step where the user enters their personal and payment
 * information to complete the booking.Encapsulates all elements and actions
 * available on this page.
 */
public class PurchasePage extends BasePage {

    // WebDriverUtils for robust element interactions
    private final WebDriverUtils webDriverUtils;

    // --- Locators ---
    private final By firstNameInput = By.id("inputName");
    private final By addressInput = By.id("address");
    private final By cityInput = By.id("city");
    private final By stateInput = By.id("state");
    private final By zipCodeInput = By.id("zipCode");
    private final By cardTypeSelect = By.id("cardType");
    private final By creditCardNumberInput = By.id("creditCardNumber");
    private final By creditCardMonthInput = By.id("creditCardMonth");
    private final By creditCardYearInput = By.id("creditCardYear");
    private final By nameOnCardInput = By.id("nameOnCard");
    private final By rememberMeCheckbox = By.id("rememberMe");
    private final By purchaseFlightButton = By.xpath("//input[@value='Purchase Flight']");

    /**
     * Constructor for the PurchasePage.
     *
     * @param driver The WebDriver instance.
     */
    public PurchasePage(WebDriver driver) {
        super(driver);
        this.webDriverUtils = new WebDriverUtils(driver, resolveTimeout());
        logger.info("PurchasePage initialized.");
    }

    // --- High-Level Service Method ---

    /**
     * Verifies that the Purchase page is displayed by checking
     * if the first name input field is visible.
     *
     * @return true if the first name input is displayed.
     */
    public boolean isPurchasePageDisplayed() {
        boolean isDisplayed = webDriverUtils.isElementDisplayed(firstNameInput);
        logger.info("Purchase page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Fills the entire purchase form using data from a Passenger record.
     * This encapsulates the low-level details of filling each field.
     *
     * @param passenger The Passenger record containing all necessary data.
     */
    public void fillPurchaseForm(Passenger passenger) {
        logger.info("Filling purchase form for passenger: {}", passenger.firstName());
        enterFirstName(passenger.firstName());
        enterAddress(passenger.address());
        enterCity(passenger.city());
        enterState(passenger.state());
        enterZipCode(passenger.zipCode());
        selectCardType(passenger.cardType());
        enterCardNumber(passenger.cardNumber());
        enterMonth(passenger.month());
        enterYear(passenger.year());
        enterNameOnCard(passenger.cardName());
        tickRememberMeCheckbox();
    }

    // --- Low-Level Action Methods ---

    public void enterFirstName(String firstName) {
        webDriverUtils.sendKeys(firstNameInput, firstName);
    }

    public void enterAddress(String address) {
        webDriverUtils.sendKeys(addressInput, address);
    }

    public void enterCity(String city) {
        webDriverUtils.sendKeys(cityInput, city);
    }

    public void enterState(String state) {
        webDriverUtils.sendKeys(stateInput, state);
    }

    public void enterZipCode(String zipCode) {
        webDriverUtils.sendKeys(zipCodeInput, zipCode);
    }

    public void selectCardType(String cardType) {
        webDriverUtils.selectByVisibleText(cardTypeSelect, cardType);
    }

    public void enterCardNumber(String cardNumber) {
        webDriverUtils.sendKeys(creditCardNumberInput, cardNumber);
    }

    public void enterMonth(String month) {
        webDriverUtils.sendKeys(creditCardMonthInput, month);
    }

    public void enterYear(String year) {
        webDriverUtils.sendKeys(creditCardYearInput, year);
    }

    public void enterNameOnCard(String name) {
        webDriverUtils.sendKeys(nameOnCardInput, name);
    }

    public void tickRememberMeCheckbox() {
        webDriverUtils.click(rememberMeCheckbox);
    }

    /**
     * Clicks the "Purchase Flight" button to submit the form and complete the
     * booking.
     */
    public void clickPurchaseFlightButton() {
        logger.info("Clicking on 'Purchase Flight' button");
        webDriverUtils.click(purchaseFlightButton);
    }
}