package com.project.aenko.provider.api;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import java.util.List;

/**
 * Provider abstraction. The core application depends only on this interface;
 * each anime data source (currently MyAnimeList) gets its own implementation.
 */
public interface AnimeProvider {

    String providerName();

    AnimeSearchResponse searchAnime(String query, int page, int size);

    AnimeDto getAnimeById(String externalId);

    List<RelatedAnimeDto> getRelatedAnime(String externalId);

    AnimeSearchResponse getRanking(String rankingType, int page, int size);
}
