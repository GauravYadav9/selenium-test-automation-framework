package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for AI test data generation.
 * Not a test class — standalone utility in test source tree.
 *
 * Usage:
 * # Using Maven exec plugin (from project root):
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test
 *
 * # With custom scenario:
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test \
 * -Dscenario="boundary value data — shortest names, longest addresses"
 *
 * # Using different provider:
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test \
 * -Dai.provider=openai
 */
public class AiDataRunner {

    private static final Logger logger = LogManager.getLogger(AiDataRunner.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  AI Test Data Runner");
        logger.info("========================================");

        logger.info("Provider: {}", ConfigReader.getProperty("ai.provider", "ollama"));

        String defaultScenario = "edge-case passenger data covering boundary values, " +
                "international names, and payment validation scenarios";
        String scenario = System.getProperty("scenario", defaultScenario);
        if (scenario.isBlank()) {
            scenario = defaultScenario;
        }

        int count;
        try {
            count = Integer.parseInt(System.getProperty("count",
                    ConfigReader.getProperty("ai.data.defaultCount", "10")));
        } catch (NumberFormatException e) {
            count = 10;
            logger.warn("Invalid 'count' value. Falling back to {}", count);
        }

        String fileName = System.getProperty("output", "ai-generated-passengers.csv");

        logger.info("Scenario: {}", scenario);
        logger.info("Count: {}", count);
        logger.info("Output: {}", fileName);

        AiDataGenerator generator = new AiDataGenerator();
        Path outputPath = generator.generate(count, scenario, fileName);

        logger.info("========================================");
        logger.info("  Generated {} records -> {}", count, outputPath.toAbsolutePath());
        logger.info("========================================");
    }
}
