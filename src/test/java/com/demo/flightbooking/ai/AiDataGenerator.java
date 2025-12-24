package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates structured passenger test data via LLM (Ollama or OpenAI)
 * and exports to CSV for consumption by CsvDataProvider.
 *
 * @see PassengerDataService
 * @see AiDataRunner
 */
public class AiDataGenerator {

    private static final Logger logger = LogManager.getLogger(AiDataGenerator.class);

    // CSV header matching passenger-data.csv exactly
    private static final String CSV_HEADER = "origin,destination,firstName,lastName,address,city,state,zipCode," +
            "cardType,cardNumber,month,year,cardName,age,gender";

    private final PassengerDataService service;

    /**
     * Creates an AiDataGenerator using the provider configured in
     * config.properties.
     * Reads ai.provider to determine Ollama (local) or OpenAI (cloud).
     */
    public AiDataGenerator() {
        this(AiModelFactory.create(0.2, true));
    }

    /**
     * Creates an AiDataGenerator with an explicitly provided ChatLanguageModel.
     * Used for testing with mock models or explicit provider override.
     *
     * @param model The ChatModel to use (Ollama, OpenAI, or mock).
     */
    public AiDataGenerator(ChatModel model) {
        this.service = AiServices.create(PassengerDataService.class, model);
        logger.info("AiDataGenerator initialized with model: {}", model.getClass().getSimpleName());
    }

    /**
     * Generates passenger test data and writes it to a CSV file.
     *
     * @param count    Number of passenger records to generate.
     * @param scenario Description of what kind of data to generate.
     *                 Examples:
     *                 - "edge-case payment data with boundary card numbers"
     *                 - "international passengers with diverse names"
     *                 - "negative test data with invalid months and expired years"
     * @param fileName Output CSV filename (e.g., "ai-generated-passengers.csv").
     * @return Path to the generated CSV file.
     */
    public Path generate(int count, String scenario, String fileName) {
        logger.info("Generating {} passenger records for scenario: '{}'", count, scenario);

        // Call the LLM via the AI Service
        List<PassengerDataService.PassengerData> passengers = service.generatePassengers(count, scenario);
        logger.info("LLM returned {} passenger records", passengers.size());

        // Write to CSV in the testdata directory
        Path outputPath = resolveOutputPath(fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println(CSV_HEADER);

            for (PassengerDataService.PassengerData p : passengers) {
                // Skip null records
                if (p == null) {
                    logger.warn("Skipping null passenger record from LLM response");
                    continue;
                }
                String csvRow = String.join(",",
                        sanitize(p.origin()),
                        sanitize(p.destination()),
                        sanitize(p.firstName()),
                        sanitize(p.lastName()),
                        sanitize(p.address()),
                        sanitize(p.city()),
                        sanitize(p.state()),
                        sanitize(p.zipCode()),
                        sanitize(p.cardType()),
                        sanitize(p.cardNumber()),
                        sanitize(p.month()),
                        sanitize(p.year()),
                        sanitize(p.cardName()),
                        String.valueOf(p.age()),
                        sanitize(p.gender()));
                writer.println(csvRow);
            }

            logger.info("CSV written to: {}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write CSV file: {}", outputPath, e);
            throw new RuntimeException("Failed to write AI-generated CSV", e);
        }

        return outputPath;
    }

    /**
     * Sanitizes a field value for CSV output.
     * - Replaces nulls with empty string
     * - Replaces commas with spaces to prevent column shifts
     * - Trims whitespace
     */
    private String sanitize(String value) {
        if (value == null)
            return "";
        // Replace commas to maintain CSV column integrity
        return value.trim().replace(",", " ");
    }

    /**
     * Resolves the output path for the generated CSV.
     * Reads output directory from config.properties (ai.data.outputDir).
     * Defaults to src/test/resources/testdata/ — same directory as existing data
     * files.
     */
    private Path resolveOutputPath(String fileName) {
        String outputDir = ConfigReader.getProperty("ai.data.outputDir", "testdata/");
        Path testdataDir = Paths.get("src", "test", "resources", outputDir);

        // Ensure directory exists
        try {
            Files.createDirectories(testdataDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create testdata directory", e);
        }

        return testdataDir.resolve(fileName);
    }
}
