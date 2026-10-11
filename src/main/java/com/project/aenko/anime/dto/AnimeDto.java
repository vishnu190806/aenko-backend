package com.project.aenko.anime.dto;

import java.util.List;

/**
 * Canonical AENKO anime model. The rest of the application only knows this
 * shape — never the raw MyAnimeList JSON.
 */
public record AnimeDto(
        Long id,
        String externalId,
        String provider,
        String title,
        String englishTitle,
        String synopsis,
        String type,
        String status,
        Integer episodes,
        Double score,
        String coverImage,
        List<String> genres,
        String externalUrl) {
}
