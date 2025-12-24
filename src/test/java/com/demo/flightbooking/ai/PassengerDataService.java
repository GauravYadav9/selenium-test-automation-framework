package com.demo.flightbooking.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * AI Service interface for generating structured passenger test data.
 * Enforces schema compliance and anti-hallucination constraints.
 */
public interface PassengerDataService {

    /**
     * Data record matching the Passenger model's CSV schema.
     *
     * IMPORTANT: Field order mirrors the CSV column order used by CsvDataProvider:
     * origin,destination,firstName,lastName,address,city,state,zipCode,
     * cardType,cardNumber,month,year,cardName,age,gender
     */
    record PassengerData(
            String origin,
            String destination,
            String firstName,
            String lastName,
            String address,
            String city,
            String state,
            String zipCode,
            String cardType,
            String cardNumber,
            String month,
            String year,
            String cardName,
            int age,
            String gender
    ) {}

    @SystemMessage("""
        You are a Senior QA data engineer generating test data for a Flight Booking application.

        ANTI-HALLUCINATION RULES (MANDATORY):
        - Generate ONLY valid data matching the exact schema requested.
        - Do NOT invent fields beyond what is specified.
        - Every value must be valid for its data type.
        - EVERY field must have a non-empty value. No field may be null or empty string.
        - Do NOT use commas inside any field value. Use spaces instead.

        FIELD CONSTRAINTS (ALL FIELDS ARE REQUIRED — NO EMPTY VALUES):
        - origin: REQUIRED. Must be EXACTLY one of: Paris, Philadelphia, Boston, Portland, San Diego, Mexico City
        - destination: REQUIRED. Must be EXACTLY one of: Rome, Berlin, London, Buenos Aires, Cairo, Dublin
        - firstName: 1-50 characters, realistic names. Include edge cases when asked
        - lastName: 1-50 characters, realistic names
        - address: Valid street address format (example: "742 Elm Street")
        - city: Valid city name (example: "Springfield")
        - state: 2-letter US state code (example: "IL")
        - zipCode: Exactly 5-digit string (example: "62704")
        - cardType: EXACTLY one of: Visa, MasterCard, American Express, Diner's Club
        - cardNumber: 16 digits for Visa/MasterCard, 15 digits for American Express, 14 digits for Diner's Club (no spaces, no dashes)
        - month: Two-digit string from 01 to 12
        - year: Four-digit string, current year or future (2025-2030)
        - cardName: Full name matching firstName + lastName
        - age: Integer between 18 and 99
        - gender: One of [Male, Female]

        EXAMPLE OUTPUT (one record):
        {"origin":"Boston","destination":"London","firstName":"Maria","lastName":"Garcia",
         "address":"456 Oak Ave","city":"Denver","state":"CO","zipCode":"80201",
         "cardType":"Visa","cardNumber":"4532015112830366","month":"06","year":"2027",
         "cardName":"Maria Garcia","age":34,"gender":"Female"}
        """)
    @UserMessage("""
        Generate exactly {{count}} passenger records for testing.

        CRITICAL: The origin and destination fields must NOT be empty. Pick from the allowed values.

        FOCUS: {{scenario}}

        Return the data as a JSON array of objects matching the PassengerData schema.
        """)
    List<PassengerData> generatePassengers(@V("count") int count, @V("scenario") String scenario);
}
