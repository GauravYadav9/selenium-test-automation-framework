package com.demo.flightbooking.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.demo.flightbooking.enums.EnvironmentType;

/**
 * A utility class to read configuration settings.
 *
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

    /**
     * Static block to load the properties file when the class is initialized.
     */
    static {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                logger.error("Configuration file not found: {}", CONFIG_FILE);
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(stream);
            logger.info("Configuration successfully loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to load configuration file: {}", CONFIG_FILE, e);
            throw new RuntimeException("Failed to load configuration", e);
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
        // Use the new default-aware getProperty method
        String value = getProperty(key, "0"); // Default to "0" if not found
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.error("Property '{}' value '{}' is not a valid integer. Defaulting to 0.", key, value, e);
            return 0; // Return 0 on format error
        }
    }

    /**
     * Gets the application URL based on the 'env' system property (e.g., -Denv=QA).
     * Falls back to the default 'application.url' if 'env' is not specified.
     * @return The target application URL for the test run.
     */
    public static String getApplicationUrl() {
        // 'env' is a perfect example of a system property override
        String env = System.getProperty("env");
        if (env == null || env.trim().isEmpty()) {
            logger.info("No 'env' system property provided. Using default 'application.url'.");
            // Use the default-aware method
            return getProperty("application.url", "https://blazedemo.com/");
        }

        EnvironmentType environmentType;
        try {
            environmentType = EnvironmentType.valueOf(env.toUpperCase().trim());
            logger.info("Running tests on environment: {}", environmentType);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid environment specified: '{}'. Please use one of: {}", env, (Object[]) EnvironmentType.values());
            throw new IllegalArgumentException("Invalid environment specified: " + env);
        }

        String propertyKey = environmentType.name().toLowerCase() + ".url";
        String url = getProperty(propertyKey); // Will use the new priority logic

        if (url == null || url.isEmpty()) {
            throw new RuntimeException("URL for environment '" + environmentType + "' not found in config.properties for key '" + propertyKey + "'");
        }
        return url;
    }
}