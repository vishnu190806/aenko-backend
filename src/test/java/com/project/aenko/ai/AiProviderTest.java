package com.project.aenko.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.aenko.ai.provider.OpenAiCompatibleProvider;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

class AiProviderTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    private OpenAiCompatibleProvider ollamaProvider() {
        return new OpenAiCompatibleProvider(restTemplate, true, "ollama",
                "http://localhost:11434/v1", "", "llama3.1:8b");
    }

    @Test
    void ollamaProviderIsAvailableWithoutApiKey() {
        assertThat(ollamaProvider().isAvailable()).isTrue();
        assertThat(ollamaProvider().providerName()).isEqualTo("ollama");
    }

    @Test
    void openaiProviderStillRequiresAnApiKey() {
        OpenAiCompatibleProvider missingKey = new OpenAiCompatibleProvider(
                restTemplate, true, "openai", "https://api.openai.com/v1", "", "gpt-4o-mini");
        assertThat(missingKey.isAvailable()).isFalse();

        OpenAiCompatibleProvider keyed = new OpenAiCompatibleProvider(
                restTemplate, true, "openai", "https://api.openai.com/v1", "sk-test", "gpt-4o-mini");
        assertThat(keyed.isAvailable()).isTrue();
    }

    @Test
    void disabledProviderIsNeverAvailable() {
        OpenAiCompatibleProvider disabled = new OpenAiCompatibleProvider(
                restTemplate, false, "ollama", "http://localhost:11434/v1", "", "llama3.1:8b");
        assertThat(disabled.isAvailable()).isFalse();
    }

    @Test
    void successfulModelResponseIsParsed() {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"id":"chatcmpl-1","object":"chat.completion","created":1,
                        "model":"llama3.1:8b",
                        "choices":[{"index":0,"message":{"role":"assistant",
                        "content":"You have been exploring action anime. Consider finishing your in-progress series first."},
                        "finish_reason":"stop"}]}""", MediaType.APPLICATION_JSON));

        String insight = ollamaProvider().generateInsight("Anime stats: total=3");

        assertThat(insight).contains("in-progress series");
        server.verify();
    }

    @Test
    void requestIsNonStreamingWithBoundedLength() throws Exception {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.model").value("llama3.1:8b"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.stream").value(false))
                .andExpect(MockRestRequestMatchers.jsonPath("$.max_tokens").exists())
                .andExpect(MockRestRequestMatchers.jsonPath("$.messages").isArray())
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"Keep watching."}}]}""",
                        MediaType.APPLICATION_JSON));

        ollamaProvider().generateInsight("Anime stats: total=1");

        server.verify();
    }

    @Test
    void modelNotFoundErrorSurfacesCleanly() {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"model 'missing' not found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ollamaProvider().generateInsight("Anime stats: total=1"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void connectionFailureSurfacesCleanly() {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(MockRestResponseCreators.withException(new ConnectException("refused")));

        assertThatThrownBy(() -> ollamaProvider().generateInsight("Anime stats: total=1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unavailable");
        server.verify();
    }

    @Test
    void timeoutSurfacesCleanly() {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(MockRestResponseCreators.withException(new SocketTimeoutException("timed out")));

        assertThatThrownBy(() -> ollamaProvider().generateInsight("Anime stats: total=1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unavailable");
        server.verify();
    }

    @Test
    void emptyChoicesSurfaceCleanly() {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[]}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ollamaProvider().generateInsight("Anime stats: total=1"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void unavailableProviderRefusesToGenerate() {
        OpenAiCompatibleProvider disabled = new OpenAiCompatibleProvider(
                restTemplate, false, "openai", "https://api.openai.com/v1", "", "gpt-4o-mini");
        assertThatThrownBy(() -> disabled.generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requestHelpersDoNotLeakSecrets() {
        // The provider only ever sends the key as a Bearer header, never in the
        // URL or body: assert the request line carries no secret material.
        server.expect(request -> {
                    assertThat(request.getURI().toString()).doesNotContain("sk-test");
                    assertThat(request.getURI().toString()).isEqualTo("https://api.openai.com/v1/chat/completions");
                })
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));

        OpenAiCompatibleProvider keyed = new OpenAiCompatibleProvider(
                restTemplate, true, "openai", "https://api.openai.com/v1", "sk-test", "gpt-4o-mini");
        assertThat(keyed.generateInsight("ctx")).isEqualTo("ok");
        server.verify();
    }

    @Test
    void contextIsSentAsUserMessage() throws Exception {
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.messages[0].role").value("system"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.messages[1].role").value("user"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.messages[1].content",
                        org.hamcrest.Matchers.containsString("total=5")))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));

        ollamaProvider().generateInsight("Anime stats: total=5, watching=2");

        server.verify();
    }

    private OpenAiCompatibleProvider geminiProvider(String baseUrl, String apiKey, String model) {
        return new OpenAiCompatibleProvider(restTemplate, true, "gemini", baseUrl, apiKey, model);
    }

    @Test
    void geminiProviderIsSelectedByNameAndNeedsAKey() {
        OpenAiCompatibleProvider gemini = geminiProvider("", "test-gemini-key", "");
        assertThat(gemini.isAvailable()).isTrue();
        assertThat(gemini.providerName()).isEqualTo("gemini");

        OpenAiCompatibleProvider missingKey = geminiProvider("", "", "");
        assertThat(missingKey.isAvailable()).isFalse();
        assertThatThrownBy(() -> missingKey.generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void geminiUsesDocumentedDefaultsWhenUnset() throws Exception {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer test-gemini-key"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.model").value("gemini-3.5-flash-lite"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.stream").value(false))
                .andExpect(MockRestRequestMatchers.jsonPath("$.max_tokens").exists())
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"candidates":[],"choices":[{"message":{"role":"assistant",
                        "content":"Finish your in-progress series first."}}]}""",
                        MediaType.APPLICATION_JSON));

        String insight = geminiProvider("", "test-gemini-key", "").generateInsight("Anime stats: total=3");

        assertThat(insight).contains("in-progress series");
        server.verify();
    }

    @Test
    void geminiTrailingSlashBaseUrlIsNormalized() {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));

        geminiProvider("https://generativelanguage.googleapis.com/v1beta/openai/", "test-gemini-key", "gemini-3.8-flash")
                .generateInsight("ctx");

        server.verify();
    }

    @Test
    void geminiExplicitModelAndUrlAreHonored() throws Exception {
        server.expect(MockRestRequestMatchers.requestTo("https://example.com/ai/v1/chat/completions"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.model").value("gemini-3.8-flash"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));

        geminiProvider("https://example.com/ai/v1", "test-gemini-key", "gemini-3.8-flash")
                .generateInsight("ctx");

        server.verify();
    }

    @Test
    void geminiUnauthorizedSurfacesCleanlyWithoutLeakingKey() {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":{\"code\":401,\"message\":\"API key not valid.\",\"status\":\"UNAUTHENTICATED\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiProvider("", "test-gemini-key", "").generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("test-gemini-key");
        server.verify();
    }

    @Test
    void geminiRateLimitSurfacesCleanly() {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"code\":429,\"message\":\"Quota exceeded.\",\"status\":\"RESOURCE_EXHAUSTED\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiProvider("", "test-gemini-key", "").generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void geminiServerErrorSurfacesCleanly() {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> geminiProvider("", "test-gemini-key", "").generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unavailable");
        server.verify();
    }

    @Test
    void geminiMalformedResponseSurfacesCleanly() {
        server.expect(MockRestRequestMatchers
                        .requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"unexpected":"shape"}""", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiProvider("", "test-gemini-key", "").generateInsight("ctx"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void providerDefaultsFallBackPerProvider() throws Exception {
        // Blank base URL must resolve to each provider's documented default,
        // never to a broken relative URL.
        server.expect(MockRestRequestMatchers.requestTo("http://localhost:11434/v1/chat/completions"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));
        server.expect(MockRestRequestMatchers.requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.model").value("gpt-4o-mini"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""",
                        MediaType.APPLICATION_JSON));

        ollamaProviderWithBlankUrl().generateInsight("ctx");
        new OpenAiCompatibleProvider(restTemplate, true, "openai", "", "sk-test", "")
                .generateInsight("ctx");

        server.verify();
    }

    private OpenAiCompatibleProvider ollamaProviderWithBlankUrl() {
        return new OpenAiCompatibleProvider(restTemplate, true, "ollama", "", "", "llama3.1:8b");
    }
}
