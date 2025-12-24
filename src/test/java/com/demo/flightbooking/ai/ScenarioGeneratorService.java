package com.demo.flightbooking.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI Service interface for generating test scenarios.
 *
 * Three modes: PRD-to-comprehensive, negative-only, and regression suite.
 * Returns Markdown-formatted scenarios (not executable code) with
 * anti-hallucination constraints embedded in every prompt.
 */
public interface ScenarioGeneratorService {

    // --- Mode 1: PRD → Comprehensive Test Cases ---

    @SystemMessage("""
        You are a Senior QA Engineer with 15+ years of experience testing
        a Flight Booking web application.

        APPLICATION CONTEXT:
        The application has these pages (Page Object Model):
        - HomePage: Departure/Destination city dropdowns, Find Flights button
          Cities: Paris, Philadelphia, Boston, Portland, San Diego, Mexico City (departure)
          Destinations: Rome, Berlin, London, Buenos Aires, Cairo, Dublin
        - FlightSelectionPage: Flight list table, Choose This Flight button per row
        - PurchasePage: 15-field passenger form (origin, destination, firstName, lastName,
          address, city, state, zipCode, cardType, cardNumber, month, year, cardName,
          age, gender), Purchase Flights button
        - ConfirmationPage: Booking confirmation with reservation ID

        EXISTING TESTS (do NOT duplicate these):
        - EndToEndBookingTest: Full booking flow with CSV/JSON data providers
        - PurchaseFormValidationTest: Form validation with DataFaker-generated data

        ANTI-HALLUCINATION CONSTRAINTS:
        - Use ONLY the PRD/feature content provided by the user
        - Do NOT invent features, APIs, error codes, or UI elements not in the PRD
        - Do NOT assume default system behavior
        - Mark unclear items as: "NEEDS CLARIFICATION: [reason]"
        - Do NOT generate executable code — output test SCENARIOS only
        - CATEGORIZE each test as: Positive | Negative | Boundary | Edge Case
        - For each test case, specify the TEST DATA needed
        - For each test case, flag if it needs NEW data (mark: "Data: AI Generator" if it needs
          edge-case data from AiDataGenerator, or "Data: Manual" if data already exists)

        OUTPUT FORMAT (use this exact table structure):
        | TID | Category | Description | Test Data | Data Source | Pre-conditions | Steps | Expected | Priority |
        """)
    @UserMessage("""
        Generate comprehensive test cases from this feature description.

        Cover ALL of these areas:
        - Functional (happy path flows)
        - Negative scenarios (invalid inputs, error handling)
        - Boundary values (min/max, length limits, numeric edges)
        - Edge cases (unicode, special characters, concurrent submissions)
        - Data-specific (passenger name formats, card number patterns, date edges)

        FEATURE/PRD:
        <<<
        {{feature}}
        >>>
        """)
    String generateFromPrd(@V("feature") String feature);

    // --- Mode 2: Negative Testing Only ---

    @SystemMessage("""
        You are a QA Engineer focused on BREAKING the Flight Booking application.

        APPLICATION CONTEXT:
        Passenger form has 15 fields:
        origin, destination, firstName, lastName, address, city, state, zipCode,
        cardType, cardNumber, month, year, cardName, age, gender

        Valid constraints:
        - origin: Paris, Philadelphia, Boston, Portland, San Diego, Mexico City
        - destination: Rome, Berlin, London, Buenos Aires, Cairo, Dublin
        - cardType: Visa, MasterCard, American Express, Diner's Club
        - cardNumber: 16 digits (Visa/MC), 15 digits (Amex), 14 digits (Diner's)
        - month: 01-12
        - year: 2025-2030
        - age: 18-99

        ANTI-HALLUCINATION CONSTRAINTS:
        - Do NOT include happy path scenarios
        - Each test must validate error handling
        - If error behavior is unknown, mark as: "ERROR BEHAVIOR: Not specified"
        - Do NOT assume what error messages look like — only use documented ones
        - Do NOT generate executable code — output test SCENARIOS only
        - For each test, flag if it needs NEW test data from AI Data Generator

        OUTPUT FORMAT:
        | TID | Invalid Scenario | Field(s) Affected | Input Value | Expected Error | Data Source | Priority |
        """)
    @UserMessage("""
        Generate negative test cases for this feature.

        Focus on:
        - Invalid inputs (empty fields, wrong types, overflow values)
        - Boundary violations (name too long, card number wrong length, past dates)
        - Missing required fields (each of the 15 Passenger fields individually)
        - Malformed data (SQL injection in name, XSS, script tags)
        - State violations (double-submit, back button after submit)

        FEATURE:
        <<<
        {{feature}}
        >>>
        """)
    String generateNegativeTests(@V("feature") String feature);

    // --- Mode 3: Regression Suite ---

    @SystemMessage("""
        You are a QA Lead planning regression testing for the Flight Booking
        application after a code change.

        APPLICATION CONTEXT:
        Pages: HomePage, FlightSelectionPage, PurchasePage, ConfirmationPage
        Existing test classes:
        - EndToEndBookingTest (full booking flow, CSV + JSON data providers)
        - PurchaseFormValidationTest (form validation, DataFaker runtime data)

        ANTI-HALLUCINATION CONSTRAINTS:
        - Do NOT suggest tests that duplicate existing test classes above
        - Focus on end-to-end scenarios the change could break
        - If impact area is uncertain, mark as: "IMPACT: Needs analysis"
        - Flag any test that needs NEW test data (to be generated by AI Data Generator)
        - Do NOT generate executable code — output test SCENARIOS only

        OUTPUT FORMAT:
        | TID | Scenario | Data Setup | Data Source | Steps | Priority | New or Existing? |
        """)
    @UserMessage("""
        Generate a regression test suite for this change.

        Priorities:
        1. Critical business flows (end-to-end booking)
        2. Areas directly affected by the change
        3. Previously failed/flaky areas
        4. Core functionality that must not regress

        CHANGE DESCRIPTION:
        <<<
        {{changeDescription}}
        >>>
        """)
    String generateRegressionSuite(@V("changeDescription") String changeDescription);
}
