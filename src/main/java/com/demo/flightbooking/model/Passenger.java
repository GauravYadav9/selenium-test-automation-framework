package com.demo.flightbooking.model;

import com.demo.flightbooking.utils.MaskingUtil;

/**
 * Represents an immutable passenger booking.
 *
 * The overridden toString() masks sensitive fields before logging
 * to prevent accidental exposure of payment information.
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