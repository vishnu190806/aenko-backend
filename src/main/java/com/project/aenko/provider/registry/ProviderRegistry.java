package com.project.aenko.provider.registry;

import com.project.aenko.provider.api.AnimeProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Selects the active anime provider. Today this is always MyAnimeList;
 * adding another provider later means registering it here without
 * touching the core services.
 */
@Component
public class ProviderRegistry {

    private final AnimeProvider animeProvider;

    public ProviderRegistry(@Qualifier("malAnimeProvider") AnimeProvider animeProvider) {
        this.animeProvider = animeProvider;
    }

    public AnimeProvider animeProvider() {
        return animeProvider;
    }
}
