package com.demo.flightbooking.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MaskingUtilTest {

    @Test
    public void testMaskStandardCardNumber() {
        String cardNumber = "4111222233334444";
        String expected = "************4444";
        Assert.assertEquals(MaskingUtil.maskCardNumber(cardNumber), expected);
    }

    @Test
    public void testMaskCardNumberWithSeparators() {
        String cardNumberWithSpaces = "4111 2222 3333 4444";
        String cardNumberWithDashes = "4111-2222-3333-4444";
        String expected = "************4444";
        Assert.assertEquals(MaskingUtil.maskCardNumber(cardNumberWithSpaces), expected);
        Assert.assertEquals(MaskingUtil.maskCardNumber(cardNumberWithDashes), expected);
    }

    @Test
    public void testMaskShortCardNumber() {
        String shortCard = "123";
        // The method should return a generic mask for invalid lengths
        Assert.assertEquals(MaskingUtil.maskCardNumber(shortCard), "************");
    }

    @Test
    public void testMaskNullAndEmptyCardNumber() {
        Assert.assertNull(MaskingUtil.maskCardNumber(null));
        Assert.assertEquals(MaskingUtil.maskCardNumber(""), "************");
    }

    // --- Tests for maskSensitiveValue() ---

    @Test
    public void testMaskSensitiveValueStandardCardNumber() {
        String cardNumber = "5555666677778888";
        String expected = "************8888";
        Assert.assertEquals(MaskingUtil.maskSensitiveValue(cardNumber), expected);
    }

    @Test
    public void testMaskSensitiveValuePassword() {
        String password = "MyP@ssw0rd!";
        String expected = "*******0rd!";
        Assert.assertEquals(MaskingUtil.maskSensitiveValue(password), expected);
    }

    @Test
    public void testMaskSensitiveValueNullAndShort() {
        Assert.assertEquals(MaskingUtil.maskSensitiveValue(null), "****");
        Assert.assertEquals(MaskingUtil.maskSensitiveValue(""), "****");
        Assert.assertEquals(MaskingUtil.maskSensitiveValue("123"), "****");
        Assert.assertEquals(MaskingUtil.maskSensitiveValue("1234"), "****");
    }
}
