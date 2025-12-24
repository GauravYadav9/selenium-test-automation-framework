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
 * Tests for AiDataGenerator's CSV pipeline using the actual SUT.
 *
 * Injects a stub PassengerDataService that returns canned data,
 * then verifies the real generate() method produces valid CSV output
 * with correct header, field count, sanitization, and schema compatibility.
 */
public class AiDataGeneratorTest {

    private static final String TEST_OUTPUT_FILE = "test-ai-output.csv";

    /**
     * Verifies that AiDataGenerator.generate() produces a valid CSV file with
     * correct header and field count when given mock LLM data.
     */
    @Test
    public void testGenerateCsvProducesValidSchema() throws IOException {
        // Given: a generator backed by a stub service returning canned data
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

        AiDataGenerator generator = new AiDataGenerator((count, scenario) -> mockData);

        // When: generate CSV through the real SUT pipeline
        Path outputPath = generator.generate(2, "test scenario", TEST_OUTPUT_FILE);

        // Then: CSV file exists
        Assert.assertTrue(Files.exists(outputPath),
                "Generated CSV file should exist at: " + outputPath);

        try (BufferedReader reader = new BufferedReader(new FileReader(outputPath.toFile()))) {
            // Verify header matches expected schema
            String header = reader.readLine();
            Assert.assertEquals(header,
                    "origin,destination,firstName,lastName,address,city,state,zipCode," +
                            "cardType,cardNumber,month,year,cardName,age,gender",
                    "CSV header must match Passenger record schema");

            // Verify each data row has exactly 15 fields
            String line;
            int rowCount = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                Assert.assertEquals(fields.length, 15,
                        "Each CSV row must have exactly 15 fields. Row: " + line);
                rowCount++;
            }

            Assert.assertEquals(rowCount, 2,
                    "CSV should have exactly 2 data rows");
        }

        // Cleanup
        Files.deleteIfExists(outputPath);
    }

    /**
     * Verifies that generated CSV data fields are correctly sanitized
     * and compatible with CsvDataProvider's Passenger record mapping.
     */
    @Test
    public void testGeneratedCsvIsCompatibleWithPassengerRecord() throws IOException {
        // Given: mock data with edge-case characters
        List<PassengerDataService.PassengerData> mockData = List.of(
                new PassengerDataService.PassengerData(
                        "Portland", "Buenos Aires", "Maria", "O'Connor", "789 Pine Rd",
                        "Springfield", "IL", "62704", "American Express", "3714496353984312",
                        "01", "2026", "Maria O'Connor", 42, "Female"
                )
        );

        AiDataGenerator generator = new AiDataGenerator((count, scenario) -> mockData);

        // When: generate CSV through the real SUT
        Path outputPath = generator.generate(1, "compatibility test", TEST_OUTPUT_FILE);

        // Then: verify field content after sanitization
        try (BufferedReader reader = new BufferedReader(new FileReader(outputPath.toFile()))) {
            reader.readLine(); // skip header
            String line = reader.readLine();
            String[] fields = line.split(",", -1);

            Assert.assertEquals(fields[0].trim(), "Portland", "origin (index 0)");
            Assert.assertEquals(fields[1].trim(), "Buenos Aires", "destination (index 1)");
            Assert.assertEquals(fields[2].trim(), "Maria", "firstName (index 2)");
            Assert.assertEquals(fields[3].trim(), "O'Connor", "lastName (index 3)");
            Assert.assertEquals(fields[13].trim(), "42", "age (index 13) must be parseable as int");
            Assert.assertEquals(fields[14].trim(), "Female", "gender (index 14)");

            // Verify age is parseable
            int age = Integer.parseInt(fields[13].trim());
            Assert.assertTrue(age >= 18 && age <= 99, "Age should be between 18 and 99");
        }

        // Cleanup
        Files.deleteIfExists(outputPath);
    }
}
