package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.MaskingUtil;
import org.testng.annotations.Test;
import org.testng.Assert;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Unit tests for AI Failure Analyzer components.
 *
 * Covers MaskingUtil.maskLogContent() (PCI data stripping regex)
 * and AiFailureAnalyzer.extractAttribute() (XML attribute extraction).
 * LLM response quality is validated via FailureAnalysisRunner, not here.
 */
public class AiFailureAnalyzerTest {

    /**
     * Test stub used to ensure XML extraction tests remain independent of LLM execution.
     * Any unexpected AI invocation fails immediately.
     */
    private static final ChatModel DUMMY_MODEL = new ChatModel() {

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            throw new UnsupportedOperationException(
                    "LLM should never be invoked during XML extraction tests.");
        }
    };

    // =====================================================================
    // MaskingUtil.maskLogContent() Tests
    // =====================================================================

    @Test(description = "Masks 16-digit credit card numbers in log content")
    public void testMaskLogContent_masksCardNumbers() {
        String input = "Processing payment with card 4111111111111111 for booking";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertFalse(result.contains("4111111111111111"),
                "Card number should be masked");
        Assert.assertTrue(result.contains("****-****-****-****"),
                "Should contain mask pattern");
        Assert.assertTrue(result.contains("Processing payment"),
                "Non-sensitive text should remain unchanged");
    }

    @Test(description = "Masks 13-digit card numbers (minimum PCI length)")
    public void testMaskLogContent_masks13DigitCard() {
        String input = "Card: 4000000000001";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertFalse(result.contains("4000000000001"),
                "13-digit card number should be masked");
    }

    @Test(description = "Masks CVV patterns in logs")
    public void testMaskLogContent_masksCvv() {
        String input = "Payment details: cvv=123, amount=500";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertFalse(result.contains("cvv=123"),
                "CVV value should be masked");
        Assert.assertTrue(result.contains("cvv=****"),
                "Should contain masked CVV");
        Assert.assertTrue(result.contains("amount=500"),
                "Non-CVV numeric values should remain");
    }

    @Test(description = "Masks API key patterns in logs")
    public void testMaskLogContent_masksApiKeys() {
        String input = "Connecting with api_key=abc123def456ghi789jkl012mno345pqr";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertFalse(result.contains("abc123def456ghi789jkl012mno345pqr"),
                "API key should be masked");
        Assert.assertTrue(result.contains("api_key=****"),
                "Should contain masked API key");
    }

    @Test(description = "Does NOT mask timestamps")
    public void testMaskLogContent_preservesTimestamps() {
        String input = "2026-04-06 16:14:38.417 [main] INFO - Test started";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertEquals(result, input,
                "Timestamps with separators should NOT be masked");
    }

    @Test(description = "Does NOT mask short numbers (ZIP codes, port numbers)")
    public void testMaskLogContent_preservesShortNumbers() {
        String input = "Server on port 8080, ZIP 10001, thread-15";
        String result = MaskingUtil.maskLogContent(input);

        Assert.assertEquals(result, input,
                "Short numbers should NOT be masked");
    }

    @Test(description = "Handles null input gracefully")
    public void testMaskLogContent_nullInput() {
        Assert.assertNull(MaskingUtil.maskLogContent(null),
                "Null input should return null");
    }

    @Test(description = "Handles empty input gracefully")
    public void testMaskLogContent_emptyInput() {
        Assert.assertEquals(MaskingUtil.maskLogContent(""), "",
                "Empty input should return empty string");
    }

    // =====================================================================
    // AiFailureAnalyzer.extractAttribute() Tests
    // =====================================================================

    @Test(description = "Extracts test count attributes from surefire XML")
    public void testExtractAttribute_validXml() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testng-results skipped="1" failed="2" ignored="0" total="10" passed="7">
            </testng-results>
            """;
        AiFailureAnalyzer analyzer = new AiFailureAnalyzer(DUMMY_MODEL);

        Assert.assertEquals(analyzer.extractAttribute(xml, "total"), 10);
        Assert.assertEquals(analyzer.extractAttribute(xml, "passed"), 7);
        Assert.assertEquals(analyzer.extractAttribute(xml, "failed"), 2);
        Assert.assertEquals(analyzer.extractAttribute(xml, "skipped"), 1);
    }

    @Test(description = "Returns 0 for missing attributes")
    public void testExtractAttribute_missingAttribute() {
        String xml = "<testng-results total=\"5\"></testng-results>";

        AiFailureAnalyzer analyzer = new AiFailureAnalyzer(DUMMY_MODEL);

        Assert.assertEquals(analyzer.extractAttribute(xml, "nonexistent"), 0,
                "Missing attribute should return 0");
    }
}
