package com.demo.flightbooking.utils;

import com.demo.flightbooking.model.Passenger;
import org.testng.annotations.DataProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides passenger test data from CSV files for data-driven execution.
 */
public class CsvDataProvider {

    private static final String CSV_FILE = ConfigReader.getProperty("data.file.passengers.csv");

    /**
     * TestNG DataProvider method that reads passenger data from a CSV file.
     * It skips the header row and converts each subsequent row into a Passenger object.
     *
     * @return A 2D Object array where each inner array contains a single Passenger object.
     */
    @DataProvider(name = "passengerCsvData")
    public static Object[][] provideCsvData() throws Exception {
        List<Passenger> passengerList = new ArrayList<>();

        InputStream is = CsvDataProvider.class.getClassLoader().getResourceAsStream(CSV_FILE);
        if (is == null) {
            throw new RuntimeException("CSV file not found on classpath: " + CSV_FILE);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 15) {
                    throw new IllegalArgumentException("CSV row has insufficient columns: " + line + " (expected at least 15, found " + fields.length + ")");
                }

                Passenger passenger = new Passenger(
                    fields[0].trim(),  // origin
                    fields[1].trim(),  // destination
                    fields[2].trim(),  // firstName
                    fields[3].trim(),  // lastName
                    fields[4].trim(),  // address
                    fields[5].trim(),  // city
                    fields[6].trim(),  // state
                    fields[7].trim(),  // zipCode
                    fields[8].trim(),  // cardType
                    fields[9].trim(),  // cardNumber
                    fields[10].trim(), // month
                    fields[11].trim(), // year
                    fields[12].trim(), // cardName
                    Integer.parseInt(fields[13].trim()), // age
                    fields[14].trim()  // gender
                );

                passengerList.add(passenger);
            }
        }

        Object[][] result = new Object[passengerList.size()][1];
        for (int i = 0; i < passengerList.size(); i++) {
            result[i][0] = passengerList.get(i);
        }
        return result;
    }

    /**
     * TestNG DataProvider method that reads route data from routes.csv.
     * Provides departure and destination cities for flight search tests.
     *
     * @return A 2D Object array where each inner array contains departureCity and destinationCity.
     */
    @DataProvider(name = "routesData")
    public static Object[][] provideRoutesData() throws Exception {
        List<Object[]> routes = new ArrayList<>();

        InputStream is = CsvDataProvider.class.getClassLoader().getResourceAsStream("testdata/routes.csv");
        if (is == null) {
            throw new RuntimeException("CSV file not found on classpath: testdata/routes.csv");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length >= 2) {
                    routes.add(new Object[]{fields[0].trim(), fields[1].trim()});
                }
            }
        }

        return routes.toArray(new Object[0][]);
    }
}
