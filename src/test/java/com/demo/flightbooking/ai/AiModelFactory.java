package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Centralized factory for LangChain4j ChatModel instances.
 * Reads provider configuration from config.properties and builds
 * the appropriate model (Ollama or OpenAI).
 *
 * Supports per-consumer customization via temperature and capability
 * parameters.
 */
public class AiModelFactory {

    private static final Logger logger = LogManager.getLogger(AiModelFactory.class);

    private AiModelFactory() {
        // Utility class — prevent instantiation
    }

    /**
     * Creates a ChatModel with default settings (temperature 0.2, no JSON schema).
     */
    public static ChatModel create() {
        return create(0.2, false);
    }

    /**
     * Creates a ChatModel with customizable temperature and JSON schema support.
     *
     * @param temperature       LLM temperature (lower = more deterministic)
     * @param requireJsonSchema If true, enables JSON schema response format (Ollama
     *                          only)
     */
    public static ChatModel create(double temperature, boolean requireJsonSchema) {
        String provider = ConfigReader.getProperty("ai.provider", "ollama");

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                String apiKey = System.getenv("OPENAI_API_KEY");
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalStateException(
                            "OPENAI_API_KEY environment variable is not set. " +
                                    "Set it or switch to ai.provider=ollama in config.properties");
                }
                String model = ConfigReader.getProperty("ai.openai.model", "gpt-4o");
                logger.info("Using OpenAI provider with model: {}", model);
                yield OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temperature)
                        .build();
            }
            case "ollama" -> {
                String baseUrl = ConfigReader.getProperty("ai.ollama.baseUrl", "http://localhost:11434");
                String model = ConfigReader.getProperty("ai.ollama.model", "llama3.2");
                logger.info("Using Ollama provider at {} with model: {}", baseUrl, model);

                var builder = OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(model)
                        .temperature(temperature)
                        .timeout(Duration.ofSeconds(300));

                if (requireJsonSchema) {
                    builder.supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
                }

                yield builder.build();
            }
            default -> throw new IllegalArgumentException(
                    "Unknown AI provider: " + provider +
                            ". Supported: 'ollama' (local, free) or 'openai' (cloud, paid)");
        };
    }
    
    /**
    * Returns the configured model name based on the active provider.
    * Used for report metadata to ensure reporting matches runtime.
    */
    public static String getModelName() {
        String provider = ConfigReader.getProperty("ai.provider", "ollama");
        return switch (provider.toLowerCase()) {
            case "openai" -> ConfigReader.getProperty("ai.openai.model", "gpt-4o");
            case "ollama" -> ConfigReader.getProperty("ai.ollama.model", "llama3.2");
            default -> throw new IllegalArgumentException("Unknown AI provider: " + provider +
                        ". Supported: 'ollama' (local, free) or 'openai' (cloud, paid)");
        };
    }
}
