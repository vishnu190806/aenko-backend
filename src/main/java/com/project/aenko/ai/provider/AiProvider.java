package com.project.aenko.ai.provider;

/**
 * Small abstraction over the AI backend. The default setup calls an
 * OpenAI-compatible HTTP endpoint; tests and offline runs use the fallback
 * without ever touching the network.
 */
public interface AiProvider {

    boolean isAvailable();

    String providerName();

    String generateInsight(String userContext);
}
