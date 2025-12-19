package com.demo.flightbooking.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import org.testng.annotations.DataProvider;

import com.demo.flightbooking.model.Passenger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Provides test data to TestNG tests by reading from a JSON file.
 * This class uses the Gson library to parse the JSON into a list of Java
 * objects.
 */
public class JsonDataProvider {

    private static final String JSON_FILE = ConfigReader.getProperty("data.file.passengers.json");

    /**
     * TestNG DataProvider method that reads passenger data from a JSON file.
     * It uses Gson to deserialize the JSON array into a list of Passenger objects.
     *
     * @return A 2D Object array where each inner array contains a single Passenger
     * object.
     */
    @DataProvider(name = "passengerData")
    public static Object[][] getPassengerData() throws Exception {
        Gson gson = new Gson();
        Type type = new TypeToken<Passenger[]>() {
        }.getType();

        // Use getResourceAsStream for classpath resources
        InputStream is = JsonDataProvider.class.getClassLoader().getResourceAsStream(JSON_FILE);
        if (is == null) {
            throw new RuntimeException("JSON file not found on classpath: " + JSON_FILE);
        }

        try (InputStreamReader reader = new InputStreamReader(is)) {
            Passenger[] passengers = gson.fromJson(reader, type);

            Object[][] data = new Object[passengers.length][1];
            for (int i = 0; i < passengers.length; i++) {
                data[i][0] = passengers[i];
            }
            return data;
        }
    }
}