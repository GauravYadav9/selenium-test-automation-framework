package com.demo.flightbooking.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for capturing browser screenshots.
 */
public class ScreenshotUtils {

    private static final Logger logger = LogManager.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String SCREENSHOT_DIR = "reports/screenshots/";

    private ScreenshotUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Captures a screenshot of the current browser window and saves it to a file.
     * The screenshot is saved in the 'reports/screenshots' directory with a
     * unique name based on the browser, test name and a timestamp.
     *
     * @param driver   The WebDriver instance.
     * @param testName The name of the test for which the screenshot is being taken.
     * @return The relative path to the saved screenshot file for embedding in reports.
     */
    public static String captureScreenshot(WebDriver driver, String testName) {
        // Create folder if it doesn’t exist
        File dir = new File(SCREENSHOT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create screenshot directory: " + SCREENSHOT_DIR);
        }

        String browser = ConfigReader.getProperty("browser", "chrome");
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String threadId = String.valueOf(Thread.currentThread().threadId());
        String fileName = browser + "_" + testName + "_" + timestamp + "_" + threadId + ".png";
        String relativePath = "screenshots/" + fileName;
        String fullPath = SCREENSHOT_DIR + fileName;

        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(srcFile, new File(fullPath));
            logger.info("Screenshot saved: {}", fullPath);
        } catch (IOException e) {
            logger.error("Failed to save screenshot to {}", fullPath, e);
        }

        // Only return the relative path
        return relativePath.replace("\\", "/"); // important for Windows
    }
}
