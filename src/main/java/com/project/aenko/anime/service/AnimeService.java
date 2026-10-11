package com.project.aenko.anime.service;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.entity.MediaExternalId;
import com.project.aenko.anime.repository.MediaExternalIdRepository;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.provider.registry.ProviderRegistry;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anime use-cases. Controllers talk to this service, which talks to the
 * provider abstraction — never to MAL directly.
 */
@Service
public class AnimeService {

    private static final Logger log = LoggerFactory.getLogger(AnimeService.class);

    private final ProviderRegistry providerRegistry;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;

    public AnimeService(ProviderRegistry providerRegistry, MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository) {
        this.providerRegistry = providerRegistry;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Cacheable(cacheNames = "animeSearch", key = "#query + '-' + #page + '-' + #size")
    public AnimeSearchResponse search(String query, int page, int size) {
        return providerRegistry.animeProvider().searchAnime(query, page, size);
    }

    @Cacheable(cacheNames = "animeDetail", key = "#externalId")
    public AnimeDto detail(String externalId) {
        return providerRegistry.animeProvider().getAnimeById(externalId);
    }

    @Cacheable(cacheNames = "animeRanking", key = "#rankingType + '-' + #page + '-' + #size")
    public AnimeSearchResponse ranking(String rankingType, int page, int size) {
        return providerRegistry.animeProvider().getRanking(rankingType, page, size);
    }

    public List<RelatedAnimeDto> related(String externalId) {
        return providerRegistry.animeProvider().getRelatedAnime(externalId);
    }

    /**
     * Returns the internal {@link Media} for a provider anime, importing it
     * from the provider on first use.
     */
    @Transactional
    public Media getOrImport(String provider, String externalId) {
        String providerName = (provider == null || provider.isBlank()) ? "MAL" : provider.toUpperCase();
        return externalIdRepository.findByProviderAndExternalId(providerName, externalId)
                .map(link -> mediaRepository.findById(link.getMediaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Anime not found")))
                .orElseGet(() -> importFromProvider(providerName, externalId));
    }

    private Media importFromProvider(String providerName, String externalId) {
        AnimeDto dto = providerRegistry.animeProvider().getAnimeById(externalId);
        Media media = new Media();
        media.setTitle(dto.title());
        media.setEnglishTitle(dto.englishTitle());
        media.setSynopsis(dto.synopsis());
        media.setType(dto.type());
        media.setStatus(dto.status());
        media.setEpisodes(dto.episodes());
        media.setScore(dto.score());
        media.setCoverImage(dto.coverImage());
        media.setGenres(dto.genres() == null ? null : String.join(",", dto.genres()));
        media.setUpdatedAt(Instant.now());
        media = mediaRepository.save(media);

        MediaExternalId link = new MediaExternalId();
        link.setMediaId(media.getId());
        link.setProvider(providerName);
        link.setExternalId(externalId);
        link.setExternalUrl(dto.externalUrl());
        link.setLastSyncedAt(Instant.now());
        externalIdRepository.save(link);
        log.info("Imported anime '{}' ({}:{}) as media {}", dto.title(), providerName, externalId, media.getId());
        return media;
    }
}
