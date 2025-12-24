package com.demo.flightbooking.ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for the AI Failure Analyzer.
 * Not a test class — standalone utility in test source tree.
 *
 * Prerequisites: run tests first (mvn test), ensure Ollama is running.
 *
 * Usage:
 *   IDE: Right-click → Run FailureAnalysisRunner.main()
 *   CLI: mvn exec:java -Dexec.mainClass="com.demo.flightbooking.ai.FailureAnalysisRunner"
 *
 * Output: target/failure-analysis-report.md
 */
public class FailureAnalysisRunner {

    private static final Logger logger = LogManager.getLogger(FailureAnalysisRunner.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  AI Failure Analysis Runner");
        logger.info("========================================");

        try {
            AiFailureAnalyzer analyzer = new AiFailureAnalyzer();
            Path reportPath = analyzer.analyze();

            if (reportPath != null) {
                logger.info("========================================");
                logger.info("  Report generated: {}", reportPath.toAbsolutePath());
                logger.info("========================================");
            } else {
                logger.info("========================================");
				logger.info("  No report generated — no failures found.");
                logger.info("========================================");
            }
        } catch (Exception e) {
			logger.info("========================================");
            logger.error("Execution failed: {}", e.getMessage(), e);
            logger.info("========================================");
        }
    }
}
