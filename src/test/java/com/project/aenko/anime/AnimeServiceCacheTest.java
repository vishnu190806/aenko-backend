package com.project.aenko.anime;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.service.AnimeService;
import com.project.aenko.provider.api.AnimeProvider;
import com.project.aenko.provider.registry.ProviderRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class AnimeServiceCacheTest {

    static final AtomicInteger SEARCH_CALLS = new AtomicInteger();

    @TestConfiguration
    static class StubProviderConfig {
        @Bean
        @Primary
        ProviderRegistry stubRegistry() {
            AnimeProvider stub = new AnimeProvider() {
                @Override
                public String providerName() {
                    return "STUB";
                }

                @Override
                public AnimeSearchResponse searchAnime(String query, int page, int size) {
                    SEARCH_CALLS.incrementAndGet();
                    return new AnimeSearchResponse(List.of(), page, size, false);
                }

                @Override
                public AnimeDto getAnimeById(String externalId) {
                    return new AnimeDto(null, externalId, "STUB", "Stub Anime", "Stub Anime EN",
                            "Synopsis", "tv", "finished_airing", 12, 8.0, "cover.jpg",
                            List.of("Action"), "https://example.com/" + externalId);
                }

                @Override
                public List<com.project.aenko.anime.dto.RelatedAnimeDto> getRelatedAnime(String externalId) {
                    return List.of();
                }

                @Override
                public AnimeSearchResponse getRanking(String rankingType, int page, int size) {
                    return new AnimeSearchResponse(List.of(), page, size, false);
                }
            };
            return new ProviderRegistry(stub);
        }
    }

    @Autowired
    private AnimeService animeService;

    @Autowired
    private com.project.aenko.anime.repository.MediaRepository mediaRepository;

    @Test
    void repeatedSearchIsServedFromCache() {
        SEARCH_CALLS.set(0);
        animeService.search("naruto-cache-probe", 0, 10);
        animeService.search("naruto-cache-probe", 0, 10);
        assertThat(SEARCH_CALLS.get()).isEqualTo(1);
    }

    @Test
    @Transactional
    void importCreatesInternalMediaAndReusesIt() {
        long before = mediaRepository.count();
        com.project.aenko.anime.entity.Media first = animeService.getOrImport("STUB", "777");
        com.project.aenko.anime.entity.Media second = animeService.getOrImport("STUB", "777");
        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(first.getTitle()).isEqualTo("Stub Anime");
        assertThat(mediaRepository.count()).isEqualTo(before + 1);
    }
}
