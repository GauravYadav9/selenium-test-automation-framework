package com.demo.flightbooking.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import com.demo.flightbooking.utils.WebDriverUtils;

import java.util.List;

/**
 * Represents the Home Page of the flight booking application.
 * This class contains WebElements and methods to interact with the flight
 * search functionality.
 */
public class HomePage extends BasePage {

    // WebDriverUtils instance for robust interactions
    private final WebDriverUtils webDriverUtils;

    // Locators
    private final By departFromDropdown = By.name("fromPort");
    private final By arriveAtDropdown = By.name("toPort");
    private final By findFlightsButton = By.cssSelector("input[type='submit']");

    /**
     * Constructor for the HomePage.
     *
     * @param driver The WebDriver instance.
     */
    public HomePage(WebDriver driver) {
        super(driver);
        this.webDriverUtils = new WebDriverUtils(driver, resolveTimeout());
        logger.info("HomePage initialized.");
    }

    /**
     * Verifies that the Home page is displayed by checking
     * if the departure dropdown is visible.
     *
     * @return true if the departure dropdown is displayed.
     */
    public boolean isHomePageDisplayed() {
        boolean isDisplayed = webDriverUtils.isElementDisplayed(departFromDropdown);
        logger.info("Home page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Retrieves a list of available departure cities from the dropdown.
     *
     * @return A list of strings representing the available departure cities.
     */
    public List<String> getAvailableDepartCities() {
        logger.debug("Getting available departure cities.");
        WebElement departFromElement = webDriverUtils.findElement(departFromDropdown); // Use WebDriverUtils
        Select select = new Select(departFromElement);
        return select.getOptions().stream()
                .map(WebElement::getText)
                .toList();
    }

    /**
     * Selects the departure city from the dropdown.
     *
     * @param city The city to select (e.g., "Paris", "Boston").
     */
    public void selectDepartFromCity(String city) {
        logger.info("Selecting departure city: {}", city);
        webDriverUtils.selectByVisibleText(departFromDropdown, city);
    }

    /**
     * Retrieves a list of available arrival cities from the dropdown.
     *
     * @return A list of strings representing the available arrival cities.
     */
    public List<String> getAvailableArriveCities() {
        logger.debug("Getting available arrival cities.");
        WebElement arriveAtElement = webDriverUtils.findElement(arriveAtDropdown);
        Select select = new Select(arriveAtElement);
        return select.getOptions().stream()
                .map(WebElement::getText)
                .toList();
    }

    /**
     * Selects the arrival city from the dropdown.
     *
     * @param city The city to select (e.g., "London", "Rome").
     */
    public void selectArriveAtCity(String city) {
        logger.info("Selecting arrival city: {}", city);
        webDriverUtils.selectByVisibleText(arriveAtDropdown, city);
    }

    /**
     * Clicks the "Find Flights" button to proceed to the flight selection page.
     */
    public void clickFindFlightsButton() {
        logger.info("Clicking Find Flights button.");
        webDriverUtils.click(findFlightsButton);
    }

    /**
     * Selects the departure and destination cities from the dropdowns and
     * submits the form to find available flights.
     *
     * @param departCity      The city of departure (e.g., "Boston").
     * @param arriveCity The city of arrival (e.g., "London").
     */
    public void findFlights(String departCity, String arriveCity) {
        logger.info("Performing flight search from {} to {}.", departCity, arriveCity);
        selectDepartFromCity(departCity);
        selectArriveAtCity(arriveCity);
        clickFindFlightsButton();
        logger.info("Flight search initiated.");
    }
}