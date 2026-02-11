package com._xdev.satorn.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for managing different LLM providers (OpenAI and Groq)
 * Groq uses OpenAI-compatible API endpoint for integration
 */
@Component
public class LLMFactory {

    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.model:mixtral-8x7b-32768}")
    private String groqModel;

    @Value("${ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.1:70b}")
    private String ollamaModel;

    @Value("${ollama.api-key:ollama}")
    private String ollamaApiKey;

    private ChatClient groqClient;
    private ChatClient ollamaClient;

    /**
     * Get OpenAI chat client for complex reasoning tasks
     */
    public ChatClient getOpenAiClient() {
        if (openAiChatModel != null) {
            return ChatClient.builder(openAiChatModel).build();
        }
        throw new IllegalStateException("OpenAI chat model not configured");
    }

    /**
     * Get Groq chat client for fast, cost-effective inference.
     * Uses OpenAI-compatible API endpoint: https://api.groq.com/openai/v1 [web:38][web:39][web:52]
     */
    public ChatClient getGroqClient() {
        if (!groqApiKey.isBlank()) {
            if (groqClient == null) {
                // Create OpenAI-compatible API instance with Groq's endpoint
                // Spring AI appends "/v1/chat/completions", so base must stop at "/openai"
                OpenAiApi groqApi = new OpenAiApi("https://api.groq.com/openai", groqApiKey);

                OpenAiChatOptions groqOptions = OpenAiChatOptions.builder()
                        .withModel(groqModel)
                        .withTemperature(0.7)
                        .build();

                OpenAiChatModel groqChatModel = new OpenAiChatModel(groqApi, groqOptions);
                groqClient = ChatClient.builder(groqChatModel).build();
            }
            return groqClient;
        }
        throw new IllegalStateException("Groq API key not configured");
    }

    /**
     * Get Ollama chat client (OpenAI-compatible endpoint).
     */
    public ChatClient getOllamaClient() {
        if (isOllamaAvailable()) {
            if (ollamaClient == null) {
                OpenAiApi ollamaApi = new OpenAiApi(ollamaBaseUrl, ollamaApiKey);

                OpenAiChatOptions ollamaOptions = OpenAiChatOptions.builder()
                        .withModel(ollamaModel)
                        .withTemperature(0.7)
                        .build();

                OpenAiChatModel ollamaChatModel = new OpenAiChatModel(ollamaApi, ollamaOptions);
                ollamaClient = ChatClient.builder(ollamaChatModel).build();
            }
            return ollamaClient;
        }
        throw new IllegalStateException("Ollama chat model not configured");
    }

    /**
     * Get appropriate client based on task type
     * - CLAIM_EXTRACTION: Groq (fast, cost-effective)
     * - VERIFICATION: Groq
     * - SYNTHESIS: Groq
     * - CHAT: Groq (balanced, fast)
     * - CATEGORY_TAGGING: Groq (fast)
     * - VISION: OpenAI (OpenAI-only path)
     */
    public ChatClient getClientForTask(TaskType taskType) {
        return switch (taskType) {
            case CLAIM_EXTRACTION -> {
                if (isGroqAvailable()) {
                    yield getGroqClient();
                }
                if (isOllamaAvailable()) {
                    yield getOllamaClient();
                }
                throw new IllegalStateException("No non-OpenAI model configured for claim extraction");
            }
            case VERIFICATION -> {
                if (isGroqAvailable()) {
                    yield getGroqClient();
                }
                throw new IllegalStateException("Groq is required for verification but is not configured");
            }
            case SYNTHESIS -> {
                if (isGroqAvailable()) {
                    yield getGroqClient();
                }
                throw new IllegalStateException("Groq is required for synthesis but is not configured");
            }
            case CHAT -> {
                if (isGroqAvailable()) {
                    yield getGroqClient();
                }
                if (isOllamaAvailable()) {
                    yield getOllamaClient();
                }
                throw new IllegalStateException("No non-OpenAI model configured for chat");
            }
            case CATEGORY_TAGGING -> {
                if (isGroqAvailable()) {
                    yield getGroqClient();
                }
                if (isOllamaAvailable()) {
                    yield getOllamaClient();
                }
                throw new IllegalStateException("No non-OpenAI model configured for category tagging");
            }
            case VISION -> getOpenAiClient();
        };
    }

    public enum TaskType {
        CLAIM_EXTRACTION,
        VERIFICATION,
        SYNTHESIS,
        CHAT,
        CATEGORY_TAGGING,
        VISION
    }

    /**
     * Check if models are available
     */
    public boolean isOpenAiAvailable() {
        return openAiChatModel != null;
    }

    public boolean isGroqAvailable() {
        return !groqApiKey.isBlank();
    }

    public boolean isOllamaAvailable() {
        return ollamaEnabled && !ollamaBaseUrl.isBlank() && !ollamaModel.isBlank();
    }
}
