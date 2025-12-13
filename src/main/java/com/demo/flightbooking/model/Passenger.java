package com.demo.flightbooking.model;

import com.demo.flightbooking.utils.MaskingUtil;

/**
 * A data model representing a passenger and their booking details.
 * This is a Java Record, a modern way to create immutable data-carrier classes.
 *
 * SECURITY: The toString() method is explicitly overridden to mask sensitive data
 * like credit card numbers to prevent accidental exposure in logs and reports.
 */
public record Passenger(
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
    String gender) {

    /**
     * Overrides toString() to mask sensitive data like credit card number.
     */
    @Override
    public String toString() {
        return "Passenger[" +
               "origin='" + origin + '\'' +
               ", destination='" + destination + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", address='" + address + '\'' +
               ", city='" + city + '\'' +
               ", state='" + state + '\'' +
               ", zipCode='" + zipCode + '\'' +
               ", cardType='" + cardType + '\'' +
               ", cardNumber='" + MaskingUtil.maskCardNumber(cardNumber) + '\'' +
               ", month='" + month + '\'' +
               ", year='" + year + '\'' +
               ", cardName='" + cardName + '\'' +
               ", age=" + age +
               ", gender='" + gender + '\'' +
               ']';
    }
}