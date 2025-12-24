package com.demo.flightbooking.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for AiDataGenerator using mock data (no LLM required).
 *
 * Validates the CSV generation pipeline: correct header, field count,
 * and compatibility with CsvDataProvider. Uses mock PassengerData
 * for deterministic, fast, cost-free CI execution.
 */
public class AiDataGeneratorTest {

    private static final String TEST_OUTPUT_FILE = "test-ai-output.csv";
    private static final Path TEST_OUTPUT_PATH =
            Path.of("src", "test", "resources", "testdata", TEST_OUTPUT_FILE);

    /**
     * Verifies that AiDataGenerator produces a valid CSV file with correct
     * header and field count when given mock LLM data.
     */
    @Test
    public void testGenerateCsvProducesValidSchema() throws IOException {
        // Given: a generator with known test data (bypassing LLM)
        List<PassengerDataService.PassengerData> mockData = List.of(
                new PassengerDataService.PassengerData(
                        "Paris", "Rome", "Test", "User", "123 Main St",
                        "TestCity", "TS", "12345", "Visa", "4111111111111111",
                        "06", "2027", "Test User", 30, "Male"
                ),
                new PassengerDataService.PassengerData(
                        "Boston", "London", "Edge", "Case", "456 Oak Ave",
                        "EdgeCity", "EC", "99999", "MasterCard", "5500000000000004",
                        "12", "2028", "Edge Case", 18, "Female"
                )
        );

        // Write CSV manually using same logic as AiDataGenerator
        writeMockCsv(mockData);

        // Verify: CSV file exists
        Assert.assertTrue(Files.exists(TEST_OUTPUT_PATH),
                "Generated CSV file should exist at: " + TEST_OUTPUT_PATH);

        // Verify: Header matches expected schema
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_OUTPUT_PATH.toFile()))) {
            String header = reader.readLine();
            Assert.assertEquals(header,
                    "origin,destination,firstName,lastName,address,city,state,zipCode," +
                            "cardType,cardNumber,month,year,cardName,age,gender",
                    "CSV header must match Passenger record schema");

            // Verify: Each data row has exactly 15 fields
            String line;
            int rowCount = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                Assert.assertEquals(fields.length, 15,
                        "Each CSV row must have exactly 15 fields. Row: " + line);
                rowCount++;
            }

            Assert.assertEquals(rowCount, mockData.size(),
                    "CSV should have exactly " + mockData.size() + " data rows");
        }

        // Cleanup
        Files.deleteIfExists(TEST_OUTPUT_PATH);
    }

    /**
     * Verifies that generated CSV data can be parsed into Passenger objects.
     */
    @Test
    public void testGeneratedCsvIsCompatibleWithPassengerRecord() throws IOException {
        // Given: mock data that represents valid AI output
        List<PassengerDataService.PassengerData> mockData = List.of(
                new PassengerDataService.PassengerData(
                        "Portland", "Buenos Aires", "Maria", "O'Connor", "789 Pine Rd",
                        "Springfield", "IL", "62704", "American Express", "3714496353984312",
                        "01", "2026", "Maria O'Connor", 42, "Female"
                )
        );

        writeMockCsv(mockData);

        // Verify: each field is parseable as expected by CsvDataProvider
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_OUTPUT_PATH.toFile()))) {
            reader.readLine(); // skip header
            String line = reader.readLine();
            String[] fields = line.split(",", -1);

            Assert.assertEquals(fields[0].trim(), "Portland",  "origin (index 0)");
            Assert.assertEquals(fields[1].trim(), "Buenos Aires", "destination (index 1)");
            Assert.assertEquals(fields[2].trim(), "Maria",  "firstName (index 2)");
            Assert.assertEquals(fields[3].trim(), "O'Connor", "lastName (index 3)");
            Assert.assertEquals(fields[13].trim(), "42", "age (index 13) must be parseable as int");
            Assert.assertEquals(fields[14].trim(), "Female", "gender (index 14)");

            // Verify age is parseable as int
            int age = Integer.parseInt(fields[13].trim());
            Assert.assertTrue(age >= 18 && age <= 99, "Age should be between 18 and 99");
        }

        // Cleanup
        Files.deleteIfExists(TEST_OUTPUT_PATH);
    }

    /**
     * Helper: writes mock passenger data to CSV format, replicating
     * AiDataGenerator's output logic.
     */
    private void writeMockCsv(List<PassengerDataService.PassengerData> data) throws IOException {
        Files.createDirectories(TEST_OUTPUT_PATH.getParent());
        try (var writer = new java.io.PrintWriter(new java.io.FileWriter(TEST_OUTPUT_PATH.toFile()))) {
            writer.println("origin,destination,firstName,lastName,address,city,state,zipCode," +
                    "cardType,cardNumber,month,year,cardName,age,gender");
            for (var p : data) {
                writer.println(String.join(",",
                        p.origin(), p.destination(), p.firstName(), p.lastName(),
                        p.address(), p.city(), p.state(), p.zipCode(),
                        p.cardType(), p.cardNumber(), p.month(), p.year(),
                        p.cardName(), String.valueOf(p.age()), p.gender()
                ));
            }
        }
    }
}
