package com.project.aenko.ai.provider;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls any OpenAI-compatible {@code /chat/completions} endpoint.
 * The API key always comes from configuration, never from source code.
 * With {@code ai.provider=ollama} the provider talks to a local Ollama
 * server, which needs no cloud key: a local-only {@code "ollama"}
 * placeholder is sent as the bearer token and is ignored by Ollama.
 * With {@code ai.provider=gemini} it talks to the documented Google Gemini
 * OpenAI-compatible endpoint
 * ({@code https://generativelanguage.googleapis.com/v1beta/openai}) using
 * the API key as the bearer token.
 */
@Component
public class OpenAiCompatibleProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    /** Placeholder bearer token for keyless local providers. Ignored by Ollama. */
    static final String LOCAL_KEY_PLACEHOLDER = "ollama";

    static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";
    static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434/v1";
    static final String DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";
    static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    static final String DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite";

    private final RestTemplate aiRestTemplate;
    private final boolean enabled;
    private final boolean ollama;
    private final boolean gemini;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleProvider(RestTemplate aiRestTemplate,
            @Value("${ai.enabled:false}") boolean enabled,
            @Value("${ai.provider:openai}") String provider,
            @Value("${ai.base-url:}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:}") String model) {
        this.aiRestTemplate = aiRestTemplate;
        this.enabled = enabled;
        String normalized = provider == null ? "" : provider.trim();
        this.ollama = "ollama".equalsIgnoreCase(normalized);
        this.gemini = "gemini".equalsIgnoreCase(normalized);
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
    }

    @Override
    public boolean isAvailable() {
        return enabled && (ollama || !apiKey.isEmpty());
    }

    @Override
    public String providerName() {
        if (ollama) {
            return "ollama";
        }
        if (gemini) {
            return "gemini";
        }
        return "openai-compatible";
    }

    /** Explicit base URL, or the documented default for the active provider. */
    String effectiveBaseUrl() {
        if (!baseUrl.isEmpty()) {
            return stripTrailingSlash(baseUrl);
        }
        if (ollama) {
            return DEFAULT_OLLAMA_BASE_URL;
        }
        if (gemini) {
            return DEFAULT_GEMINI_BASE_URL;
        }
        return DEFAULT_OPENAI_BASE_URL;
    }

    /** Explicit model, or the default for the active cloud provider. */
    String effectiveModel() {
        if (!model.isEmpty()) {
            return model;
        }
        if (gemini) {
            return DEFAULT_GEMINI_MODEL;
        }
        return DEFAULT_OPENAI_MODEL;
    }

    private static String stripTrailingSlash(String url) {
        String stripped = url;
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateInsight(String userContext) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI provider is not configured");
        }
        Map<String, Object> request = Map.of(
                "model", effectiveModel(),
                "max_tokens", 300,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are AENKO, a friendly anime journey coach. "
                                        + "Give one short personalized insight (max 5 sentences) "
                                        + "based on the user's anime statistics. "
                                        + "Suggest what to watch or do next. No lists, no markdown headings."),
                        Map.of("role", "user", "content", userContext)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.isEmpty() ? LOCAL_KEY_PLACEHOLDER : apiKey);
        try {
            Map<String, Object> response = aiRestTemplate.postForObject(
                    effectiveBaseUrl() + "/chat/completions", new HttpEntity<>(request, headers), Map.class);
            if (response == null) {
                throw new IllegalStateException("Empty AI response");
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("No AI choices returned");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new IllegalStateException("No AI message content");
            }
            return message.get("content").toString().trim();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI provider call failed", ex);
            throw new IllegalStateException("AI provider unavailable", ex);
        }
    }
}
