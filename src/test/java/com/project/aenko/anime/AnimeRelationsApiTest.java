package com.project.aenko.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.exception.ProviderException;
import com.project.aenko.provider.api.AnimeProvider;
import com.project.aenko.provider.registry.ProviderRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Verifies the Anime Guide data flow: search/detail/relations are served from
 * the provider abstraction using the external MAL id, with no requirement
 * that the anime exists in the local Media table (no library import needed).
 */
class AnimeRelationsApiTest extends AenkoIntegrationTest {

    @TestConfiguration
    static class StubGuideProviderConfig {
        @Bean
        @Primary
        ProviderRegistry stubRegistry() {
            AnimeProvider stub = new AnimeProvider() {
                @Override
                public String providerName() {
                    return "MAL";
                }

                @Override
                public AnimeSearchResponse searchAnime(String query, int page, int size) {
                    return new AnimeSearchResponse(List.of(), page, size, false);
                }

                @Override
                public AnimeDto getAnimeById(String externalId) {
                    if ("404".equals(externalId)) {
                        throw new ProviderException("Anime not found", 404);
                    }
                    return new AnimeDto(null, externalId, "MAL", "Naruto", "Naruto",
                            "A ninja story", "tv", "finished_airing", 220, 8.0,
                            "cover.jpg", List.of("Action"),
                            "https://myanimelist.net/anime/" + externalId);
                }

                @Override
                public List<RelatedAnimeDto> getRelatedAnime(String externalId) {
                    if ("404".equals(externalId)) {
                        throw new ProviderException("Anime not found", 404);
                    }
                    if ("999".equals(externalId)) {
                        return List.of();
                    }
                    return List.of(
                            new RelatedAnimeDto(
                                    new AnimeDto(null, "21", "MAL", "Naruto Shippuden", null,
                                            null, "tv", "finished_airing", 500, 8.5,
                                            "cover2.jpg", List.of(),
                                            "https://myanimelist.net/anime/21"),
                                    "SEQUEL"),
                            new RelatedAnimeDto(
                                    new AnimeDto(null, "19", "MAL", "Naruto Special", null,
                                            null, "special", "finished_airing", 1, 7.0,
                                            "cover3.jpg", List.of(),
                                            "https://myanimelist.net/anime/19"),
                                    "SIDE_STORY"));
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
    private MediaRepository mediaRepository;

    @Test
    void relationsAreServedFromProviderWithoutLocalMedia() throws Exception {
        // Only the seeded curated-catalogue media may exist; serving provider
        // relationships must not import anything new locally.
        long mediaBefore = mediaRepository.count();

        mockMvc.perform(get("/api/anime/20/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].relationType").value("SEQUEL"))
                .andExpect(jsonPath("$[0].anime.title").value("Naruto Shippuden"))
                .andExpect(jsonPath("$[0].anime.externalId").value("21"))
                .andExpect(jsonPath("$[0].anime.provider").value("MAL"))
                .andExpect(jsonPath("$[1].relationType").value("SIDE_STORY"));

        // Displaying provider relationships must not import anything locally.
        assertThat(mediaRepository.count()).isEqualTo(mediaBefore);
    }

    @Test
    void missingRelationshipsReturnEmptyCollection() throws Exception {
        mockMvc.perform(get("/api/anime/999/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownAnimeRelationsReturn404() throws Exception {
        mockMvc.perform(get("/api/anime/404/relations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void detailUsesExternalMalIdAndKeepsContract() throws Exception {
        mockMvc.perform(get("/api/anime/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("20"))
                .andExpect(jsonPath("$.provider").value("MAL"))
                .andExpect(jsonPath("$.title").value("Naruto"));
    }

    @Test
    void unknownAnimeDetailReturns404() throws Exception {
        mockMvc.perform(get("/api/anime/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
