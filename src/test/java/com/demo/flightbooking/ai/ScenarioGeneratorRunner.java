package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for the AI Scenario Generator.
 * Not a test class — standalone utility in test source tree.
 *
 * Usage:
 *   # Mode 1 — PRD → Comprehensive (default):
 *   mvn exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.ai.ScenarioGeneratorRunner" \
 *     -Dexec.classpathScope=test \
 *     -Dfeature="User can select departure and destination cities and search for flights"
 *
 *   # Mode 2 — Negative Testing:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.ai.ScenarioGeneratorRunner" \
 *     -Dexec.classpathScope=test \
 *     -Dmode=negative \
 *     -Dfeature="Purchase page with 15-field passenger form"
 *
 *   # Mode 3 — Regression Suite:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.ai.ScenarioGeneratorRunner" \
 *     -Dexec.classpathScope=test \
 *     -Dmode=regression \
 *     -Dfeature="Changed card number validation to accept 13-16 digits"
 */
public class ScenarioGeneratorRunner {

    private static final Logger logger = LogManager.getLogger(ScenarioGeneratorRunner.class);

    private static final String DEFAULT_FEATURE = """
        Flight Booking Purchase Page:
        - User fills a 15-field passenger form (name, address, payment details)
        - Card types: Visa, MasterCard, American Express, Diner's Club
        - Card number must be valid for the selected card type
        - Month (01-12) and year (2025-2030) for expiry
        - User clicks 'Purchase Flight' to complete booking
        - Confirmation page shows reservation ID on success
        """;

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  AI Scenario Generator Runner");
        logger.info("========================================");

        String mode = System.getProperty("mode", "prd");
        String feature = System.getProperty("feature", DEFAULT_FEATURE);

        logger.info("Mode: {}", mode);
        logger.info("Provider: {}", ConfigReader.getProperty("ai.provider", "ollama"));

        try {
            AiScenarioGenerator generator = new AiScenarioGenerator();
            Path reportPath;

            switch (mode.toLowerCase()) {
                case "negative" -> {
                    logger.info("Generating negative test scenarios...");
                    reportPath = generator.generateNegativeTests(feature);
                }
                case "regression" -> {
                    logger.info("Generating regression suite...");
                    reportPath = generator.generateRegressionSuite(feature);
                }
                default -> {
                    if (!"prd".equalsIgnoreCase(mode)) {
                        logger.warn("Unknown mode '{}', defaulting to PRD. Valid modes: prd, negative, regression", mode);
                    }
                    logger.info("Generating comprehensive test scenarios from PRD...");
                    reportPath = generator.generateFromPrd(feature);
                }
            }

            logger.info("========================================");
            logger.info("  Report generated: {}", reportPath.toAbsolutePath());
            logger.info("========================================");

        } catch (Exception e) {
			logger.info("========================================");
            logger.error("Execution failed: {}", e.getMessage(), e);
            logger.info("========================================");
        }
    }
}
