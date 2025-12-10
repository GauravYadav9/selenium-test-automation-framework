package com.demo.flightbooking.utils;

/**
 * Utility class for masking sensitive data to prevent exposure in logs and reports.
 * This is critical for compliance with regulations like PCI DSS and GDPR.
 * The class is final to prevent extension, as it only contains static methods.
 */
public final class MaskingUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MaskingUtil() {}

    /**
     * Masks a credit card number, showing only the last 4 digits. The mask
     * character '*' is repeated to match the original length of the number.
     * It handles nulls and cleans non-digit characters before masking.
     * Example: "4111-1111 1111-1111" becomes "************4444"
     *
     * @param cardNumber The full credit card number, possibly with separators.
     * @return A masked card number, or null if the input is null.
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }

        // Normalize the input by removing all non-digit characters
        String digitsOnly = cardNumber.replaceAll("\\D", "");
        int len = digitsOnly.length();

        if (len <= 4) {
            // Return a generic mask for very short or invalid numbers
            return "************";
        }

        // Create a dynamic-length mask
        return "*".repeat(len - 4) + digitsOnly.substring(len - 4);
    }

    /**
     * Masks any sensitive value for safe logging, showing only the last 4
     * characters.
     * Unlike {@link #maskCardNumber(String)}, this does NOT strip non-digit
     * characters,
     * making it suitable for passwords, CVVs, and any arbitrary sensitive text.
     * Example: "5555666677778888" becomes "************8888"
     * Example: "MyP@ssw0rd!" becomes "*******0rd!"
     *
     * @param value The sensitive value to mask.
     * @return A masked version of the value, or "****" if null/short.
     */
    public static String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    /**
     * Masks sensitive data patterns in raw log content before sending to AI providers.
     * This is the DATA PRIVACY step in the AI failure analysis pipeline:
     *   Raw logs → maskLogContent() → Masked logs → LLM
     *
     * Applies regex-based masking for:
     * - Credit card numbers (13-19 consecutive digit sequences)
     * - CVV/CVC patterns (3-4 digits following cvv/cvc keywords)
     * - API keys and tokens (long alphanumeric strings following key/token/secret keywords)
     *
     * Design decision: We prefer over-masking over the risk of data exposure.
     * Our log timestamps use separators (2026-04-06 16:14:38.417) so they
     * don't match \b\d{13,19}\b. False positives are rare in practice.
     *
     * @param logContent Raw log text to mask.
     * @return Log text with sensitive patterns replaced, or null/empty if input is null/empty.
     */
    public static String maskLogContent(String logContent) {
        if (logContent == null || logContent.isEmpty()) {
            return logContent;
        }

        String masked = logContent;

        // Mask credit card number patterns (13-19 consecutive digits)
        masked = masked.replaceAll("\\b\\d{13,19}\\b", "****-****-****-****");

        // Mask CVV/CVC patterns (3-4 digits after cvv/cvc keyword)
        masked = masked.replaceAll("(?i)(cvv|cvc|security.?code)[=:\\s]+\\d{3,4}", "$1=****");

        // Mask potential API keys (long hex/alphanumeric after key/token/secret)
        masked = masked.replaceAll("(?i)(api.?key|token|secret)[=:\\s]+[\\w-]{20,}", "$1=****");

        return masked;
    }
}
