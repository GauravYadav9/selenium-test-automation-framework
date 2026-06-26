package com.demo.flightbooking.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.demo.flightbooking.enums.EnvironmentType;

/**
 * A utility class to read configuration settings.
 * It uses a priority-based lookup to allow runtime overrides:
 * 1. Java System Property (e.g., -Dbrowser=firefox)
 * 2. config.properties file
 * 3. A hardcoded default (if provided)
 */
public class ConfigReader {

    private static final Logger logger = LogManager.getLogger(ConfigReader.class);
    private static final Properties properties = new Properties();
    // Loads 'config.properties' from 'src/main/resources' or 'src/test/resources'
    private static final String CONFIG_FILE = System.getProperty("configFile", "config/config.properties");

    private ConfigReader() {
        // Utility class — prevent instantiation
    }

    /**
     * Static block to load the properties file when the class is initialized.
     */
    static {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(stream);
            logger.info("Configuration successfully loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration file: " + CONFIG_FILE, e);
        }
    }

    /**
     * Gets a property with a priority-based lookup:
     * 1. Java System Property (e.g., -Dkey=value)
     * 2. config.properties file
     *
     * @param key The property key to look up.
     * @param defaultValue A default value if nothing is found.
     * @return The found property value.
     */
    private static String getPropertyFromSources(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            logger.debug("Overriding property '{}' with System Property: '{}'", key, value);
            return value;
        }

        return properties.getProperty(key, defaultValue);
    }

    /**
     * Retrieves a property value by its key.
     *
     * @param key The key of the property to retrieve.
     * @return The property value as a String, or null if not found.
     */
    public static String getProperty(String key) {
        String value = getPropertyFromSources(key, null);
        if (value == null) {
            logger.warn("Property not found: {} (and no default was set)", key);
        }
        return value;
    }

    /**
     * Retrieves a property value by its key, returning a default if not found.
     *
     * @param key The key of the property to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The property value as a String.
     */
    public static String getProperty(String key, String defaultValue) {
        return getPropertyFromSources(key, defaultValue);
    }

    /**
     * Retrieves a property value and converts it to an integer.
     * Uses the default-supporting getProperty method.
     *
     * @param key The key of the property to retrieve.
     * @return The property value as an int.
     */
    public static int getPropertyAsInt(String key) {
        String value = getProperty(key, "0");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.error("Property '{}' value '{}' is not a valid integer. Defaulting to 0.", key, value, e);
            return 0;
        }
    }

    /**
     * Gets the application URL based on the 'env' system property (e.g., -Denv=QA).
     * Falls back to the default 'application.url' if 'env' is not specified.
     * @return The target application URL for the test run.
     */
    public static String getApplicationUrl() {
        String env = System.getProperty("env");
        if (env == null || env.trim().isEmpty()) {
            logger.info("No 'env' system property provided. Using default 'application.url'.");
            return getProperty("application.url", "https://blazedemo.com/");
        }

        EnvironmentType environmentType;
        try {
            environmentType = EnvironmentType.valueOf(env.toUpperCase(Locale.ENGLISH).trim());
            logger.info("Running tests on environment: {}", environmentType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid environment specified: '" + env + "'. Valid: " + Arrays.toString(EnvironmentType.values()), e);
        }

        String propertyKey = environmentType.name().toLowerCase(Locale.ENGLISH) + ".url";
        String url = getProperty(propertyKey);

        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("URL for environment '" + environmentType + "' not found in config.properties for key '" + propertyKey + "'");
        }
        return url;
    }
}