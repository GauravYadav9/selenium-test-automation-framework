package com.demo.flightbooking.ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for the AI Test Summary Generator.
 * Not a test class — standalone utility in test source tree.
 *
 * Usage:
 * CLI: mvn exec:java -Dexec.mainClass="com.demo.flightbooking.ai.TestSummaryRunner"
 *
 * Output: target/ai-summary-report.md
 */
public class TestSummaryRunner {

    private static final Logger logger = LogManager.getLogger(TestSummaryRunner.class);

    public static void main(String[] args) {

        logger.info("=== AI Test Summary Runner ===");

        try {
            AiTestSummaryGenerator generator = new AiTestSummaryGenerator();
            Path report = generator.generateSummary();

            if (report != null) {
                logger.info("Summary generated: {}", report.toAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("Summary generation failed", e);
        }
    }
}