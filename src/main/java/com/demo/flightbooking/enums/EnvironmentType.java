package com.demo.flightbooking.enums;

/**
 * An enumeration of the possible test environments (e.g., QA, STAGING).
 * This can be used to dynamically select the correct application URL
 * from the configuration file.
 */
public enum EnvironmentType {
    QA,
    STAGING,
    PRODUCTION
}