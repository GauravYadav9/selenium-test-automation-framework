package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.MaskingUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Surefire XML and automation logs to provide LLM-assisted
 * root cause analysis for test failures.
 *
 * Runs post-execution only — never during tests. Advisory output
 * (markdown report), not a pass/fail decision. If LLM provider
 * is unavailable, analysis is skipped gracefully.
 *
 * Supports Ollama (local) and OpenAI (cloud) via config.properties.
 *
 * @see FailureAnalysisService
 * @see FailureAnalysisRunner
 */
public class AiFailureAnalyzer {

    private static final Logger logger = LogManager.getLogger(AiFailureAnalyzer.class);

    // Max lines from filtered log output
    private static final int MAX_LOG_LINES = 30;

    private final FailureAnalysisService service;

    /**
     * Creates an AiFailureAnalyzer using the provider configured in
     * config.properties.
     */
    public AiFailureAnalyzer() {
        this(AiModelFactory.create());
    }

    /**
     * Creates an AiFailureAnalyzer with an explicitly provided ChatModel.
     * Used for testing with mock models or explicit provider override.
     *
     * @param model The ChatModel to use (Ollama, OpenAI, or mock).
     */
    public AiFailureAnalyzer(ChatModel model) {
        this.service = AiServices.create(FailureAnalysisService.class, model);
        logger.info("AiFailureAnalyzer initialized with model: {}", model.getClass().getSimpleName());
    }

    /**
     * Runs the full failure analysis pipeline: read XML, extract failures,
     * filter logs, mask sensitive data, call LLM, write report.
     * 
     * @return Path to the generated report, or null if no failures detected.
     */
    public Path analyze() {
        logger.info("=== AI Failure Analysis Started ===");

        // Dynamically find ALL testng-results.xml files
        // Handles: target/ (local), target-chrome/ and target-firefox/ (CI parallel)
        List<Path> xmlFiles = new ArrayList<>();
        try (var stream = Files.walk(Paths.get("."))) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().contains("surefire-reports")
                            && p.getFileName().toString().equals("testng-results.xml"))
                    .forEach(xmlFiles::add);
        } catch (IOException e) {
            logger.error("Failed to scan for Surefire XML files", e);
            return null;
        }

        if (xmlFiles.isEmpty()) {
            logger.warn("No Surefire XML files found in workspace. Run tests first.");
            return null;
        }

        logger.info("Found {} Surefire XML file(s): {}", xmlFiles.size(), xmlFiles);

        // Aggregate test counts and failure blocks across all browser XMLs
        int total = 0, passed = 0, failed = 0, skipped = 0;
        StringBuilder combinedFailures = new StringBuilder();

        for (Path xmlPath : xmlFiles) {
            try {
                String surefireXml = Files.readString(xmlPath);
                total += extractAttribute(surefireXml, "total");
                passed += extractAttribute(surefireXml, "passed");
                failed += extractAttribute(surefireXml, "failed");
                skipped += extractAttribute(surefireXml, "skipped");

                String blocks = extractFailureBlocks(surefireXml);
                if (!blocks.contains("[No failure blocks found in XML]")) {
                    combinedFailures.append(blocks).append("\n");
                }
            } catch (IOException e) {
                logger.error("Failed to read Surefire XML: {}", xmlPath, e);
            }
        }

        logger.info("Aggregated test results across {} browser(s) — Total: {}, Passed: {}, Failed: {}, Skipped: {}",
                xmlFiles.size(), total, passed, failed, skipped);

        // No failures detected across any browser
        if (failed == 0) {
            logger.info("No failures detected across any browser. Skipping AI analysis.");
            return null;
        }

        String structuredFailures = combinedFailures.toString();
        logger.info("Extracted failure blocks from {} XML file(s)", xmlFiles.size());

        // Read automation log — filtered to ERROR/WARN + failure context
        Path logPath = Paths.get("logs", "automation.log");
        String filteredLog = filterRelevantLogs(logPath, structuredFailures);

        // Mask sensitive data before sending to AI
        String maskedFailures = MaskingUtil.maskLogContent(structuredFailures);
        String maskedLog = MaskingUtil.maskLogContent(filteredLog);
        logger.info("Data masked. Failures: {} chars, Filtered log: {} chars",
                maskedFailures.length(), maskedLog.length());

        // Call the LLM with structured input
        logger.info("Sending structured data to AI for analysis...");
        String analysis;
        try {
            analysis = service.analyzeFailures(maskedFailures, maskedLog, total, passed, failed, skipped);
        } catch (Exception e) {
            logger.error("AI analysis failed (LLM error): {}", e.getMessage(), e);
            return null;
        }

        // Write the report
        Path reportPath = writeReport(analysis, total, passed, failed, skipped);
        logger.info("=== AI Failure Analysis Complete. Report: {} ===", reportPath);

        return reportPath;
    }

    /**
     * Extracts a numeric attribute from the testng-results.xml root element.
     *
     * @param xml       Full XML content
     * @param attribute Attribute name (e.g., "total", "failed")
     * @return Attribute value as int, or 0 if not found
     */
    int extractAttribute(String xml, String attribute) {
        String search = attribute + "=\"";
        int start = xml.indexOf(search);
        if (start == -1)
            return 0;

        start += search.length();
        int end = xml.indexOf("\"", start);
        if (end == -1)
            return 0;

        try {
            return Integer.parseInt(xml.substring(start, end));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse attribute '{}': {}", attribute, xml.substring(start, end));
            return 0;
        }
    }

    /**
     * Extracts structured failure blocks from testng-results.xml.
     * Parses out: Test method name, Exception class, Error message,
     * and Stack trace top.
     *
     * @param xml Full testng-results.xml content
     * @return Structured text block like:
     *         FAILURE 1: testPurchaseWithInvalidData
     *         Exception: java.lang.AssertionError
     *         Message: Purchase should have failed...
     *         StackTrace: (top 5 lines)
     */
    String extractFailureBlocks(String xml) {
        StringBuilder result = new StringBuilder();
        result.append("STRUCTURED FAILURE DATA (extracted from TestNG XML):\n");
        result.append("=".repeat(60)).append("\n\n");

        // Pattern to find <test-method> blocks with status="FAIL" (excluding is-config
        // methods)
        int failureCount = 0;
        int searchFrom = 0;

        while (true) {
            // Find next status="FAIL" occurrence
            int failIndex = xml.indexOf("status=\"FAIL\"", searchFrom);
            if (failIndex == -1)
                break;

            // Locate parent <test-method> element
            int methodStart = xml.lastIndexOf("<test-method", failIndex);
            if (methodStart == -1) {
                searchFrom = failIndex + 1;
                continue;
            }

            // Skip configuration methods
            String methodTag = xml.substring(methodStart, failIndex + 20);
            if (methodTag.contains("is-config=\"true\"")) {
                searchFrom = failIndex + 1;
                continue;
            }

            failureCount++;
            result.append("FAILURE ").append(failureCount).append(":\n");

            // Extract test name
            String name = extractXmlValue(methodTag, "name=\"");
            result.append("  Test: ").append(name != null ? name : "unknown").append("\n");

            // Find the end of this test-method block
            int methodEnd = xml.indexOf("</test-method>", failIndex);
            if (methodEnd == -1)
                methodEnd = xml.length();
            String methodBlock = xml.substring(methodStart, methodEnd);

            // Extract exception class
            String exceptionClass = extractXmlValue(methodBlock, "exception class=\"");
            result.append("  Exception: ").append(exceptionClass != null ? exceptionClass : "unknown").append("\n");

            // Extract message from CDATA
            String message = extractCdataContent(methodBlock, "<message>");
            if (message != null) {
                // Truncate long messages to first 200 chars
                String trimmed = message.trim();
                if (trimmed.length() > 200) {
                    trimmed = trimmed.substring(0, 200) + "...";
                }
                result.append("  Message: ").append(trimmed).append("\n");
            }

            // Extract top 5 lines of stack trace
            String stackTrace = extractCdataContent(methodBlock, "<full-stacktrace>");
            if (stackTrace != null) {
                String[] lines = stackTrace.trim().split("\n");
                result.append("  StackTrace (top ").append(Math.min(lines.length, 5)).append(" lines):\n");
                for (int i = 0; i < Math.min(lines.length, 5); i++) {
                    result.append("    ").append(lines[i].trim()).append("\n");
                }
            }

            result.append("\n");
            searchFrom = failIndex + 1;
        }

        if (failureCount == 0) {
            result.append("  [No failure blocks found in XML]\n");
        }

        logger.info("Extracted {} failure block(s) from XML", failureCount);
        return result.toString();
    }

    /**
     * Filters automation log to only relevant lines: ERROR, WARN, and lines
     * containing the names of failed tests.
     *
     * @param logPath            Path to automation.log
     * @param structuredFailures The extracted failure block text (used to find test
     *                           names)
     * @return Filtered log lines containing only signal
     */
    private String filterRelevantLogs(Path logPath, String structuredFailures) {
        if (!Files.exists(logPath)) {
            logger.warn("Automation log not found at {}. Analysis will use XML only.", logPath);
            return "[No log file found]";
        }

        try {
            List<String> allLines = Files.readAllLines(logPath);
            List<String> relevantLines = new ArrayList<>();

            // Extract failed test names from the structured failures block
            List<String> failedTestNames = extractFailedTestNames(structuredFailures);

            for (String line : allLines) {
                // Filter for ERROR and WARN entries
                if (line.contains(" ERROR ") || line.contains(" WARN ")) {
                    relevantLines.add(line);
                    continue;
                }

                // Keep lines that mention any failed test name
                for (String testName : failedTestNames) {
                    if (line.contains(testName)) {
                        relevantLines.add(line);
                        break;
                    }
                }
            }

            // Retain recent logs up to MAX_LOG_LINES
            if (relevantLines.size() > MAX_LOG_LINES) {
                relevantLines = relevantLines.subList(
                        relevantLines.size() - MAX_LOG_LINES, relevantLines.size());
            }

            logger.info("Log filtered: {} total lines → {} relevant lines",
                    allLines.size(), relevantLines.size());

            if (relevantLines.isEmpty()) {
                return "[No ERROR/WARN log entries found]";
            }

            return String.join("\n", relevantLines);
        } catch (IOException e) {
            logger.error("Failed to read log file: {}", logPath, e);
            return "[Error reading log file: " + e.getMessage() + "]";
        }
    }

    /**
     * Extracts test names from the structured failures block.
     * Looks for lines like " Test: testPurchaseWithInvalidData"
     */
    private List<String> extractFailedTestNames(String structuredFailures) {
        List<String> names = new ArrayList<>();
        for (String line : structuredFailures.split("\n")) {
            if (line.trim().startsWith("Test: ")) {
                String name = line.trim().substring("Test: ".length()).trim();
                if (!name.equals("unknown")) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Extracts an XML attribute value following the given start marker.
     * Reads until the next double-quote character.
     * Example: extractXmlValue(tag, "name=\"") extracts the name attribute value.
     */
    private String extractXmlValue(String xml, String startMarker) {
        int start = xml.indexOf(startMarker);
        if (start == -1)
            return null;
        start += startMarker.length();
        int end = xml.indexOf("\"", start);
        if (end == -1)
            return null;
        return xml.substring(start, end);
    }

    /**
     * Extracts CDATA content after a given XML tag.
     * Example: extractCdataContent(block, "<message>") extracts the message CDATA.
     */
    private String extractCdataContent(String xml, String tag) {
        int tagStart = xml.indexOf(tag);
        if (tagStart == -1)
            return null;

        int cdataStart = xml.indexOf("<![CDATA[", tagStart);
        if (cdataStart == -1)
            return null;
        cdataStart += "<![CDATA[".length();

        int cdataEnd = xml.indexOf("]]>", cdataStart);
        if (cdataEnd == -1)
            return null;

        return xml.substring(cdataStart, cdataEnd);
    }

    /**
     * Writes the AI analysis to a markdown report file.
     * Adds a header with timestamp and test counts for context.
     *
     * @return Path to the written report file
     */
    private Path writeReport(String analysis, int total, int passed, int failed, int skipped) {
        Path reportPath = Paths.get("target", "failure-analysis-report.md");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String report = """
                # AI Failure Analysis Report

                **Generated:** %s
                **Test Results:** %d total | %d passed | %d failed | %d skipped

                ---

                %s
                """.formatted(timestamp, total, passed, failed, skipped, analysis);

        try {
            Files.writeString(reportPath, report);
            logger.info("Report written to: {}", reportPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write report: {}", reportPath, e);
        }

        return reportPath;
    }
}
