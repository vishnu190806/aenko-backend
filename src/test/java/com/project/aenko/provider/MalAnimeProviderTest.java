package com.project.aenko.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.common.exception.ProviderException;
import com.project.aenko.provider.mal.MalAnimeProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MalAnimeProviderTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private MalAnimeProvider provider;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        provider = new MalAnimeProvider(restTemplate, "https://api.myanimelist.net/v2", "test-client-id");
    }

    @Test
    void searchMapsNodesToCanonicalModel() {
        server.expect(request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
                    assertThat(request.getURI().toString()).contains("/anime?q=naruto");
                    assertThat(request.getHeaders().getFirst("X-MAL-CLIENT-ID")).isEqualTo("test-client-id");
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"data":[{"node":{"id":20,"title":"Naruto","alternative_titles":{"en":"Naruto"},
                                "synopsis":"A ninja story","mean":8.0,"media_type":"tv","status":"finished_airing",
                                "num_episodes":220,"genres":[{"id":1,"name":"Action"}],
                                "main_picture":{"medium":"m.jpg","large":"l.jpg"}}}],
                                "paging":{}}""", MediaType.APPLICATION_JSON));

        AnimeSearchResponse response = provider.searchAnime("naruto", 0, 10);

        assertThat(response.content()).hasSize(1);
        AnimeDto anime = response.content().get(0);
        assertThat(anime.externalId()).isEqualTo("20");
        assertThat(anime.provider()).isEqualTo("MAL");
        assertThat(anime.title()).isEqualTo("Naruto");
        assertThat(anime.englishTitle()).isEqualTo("Naruto");
        assertThat(anime.episodes()).isEqualTo(220);
        assertThat(anime.genres()).containsExactly("Action");
        assertThat(anime.coverImage()).isEqualTo("l.jpg");
        assertThat(anime.externalUrl()).isEqualTo("https://myanimelist.net/anime/20");
        server.verify();
    }

    @Test
    void searchRejectsEmptyQuery() {
        assertThatThrownBy(() -> provider.searchAnime("  ", 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detailReturnsAnime() {
        server.expect(request -> {
                    assertThat(request.getURI().toString()).contains("/anime/20?");
                    // The details lookup must request the supported related_anime
                    // field so relationship data is available to the guide.
                    assertThat(request.getURI().toString()).contains("related_anime");
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"id":20,"title":"Naruto","media_type":"tv","status":"finished_airing",
                                "num_episodes":220,"mean":8.0,"genres":[]}""", MediaType.APPLICATION_JSON));

        AnimeDto anime = provider.getAnimeById("20");

        assertThat(anime.title()).isEqualTo("Naruto");
        assertThat(anime.externalId()).isEqualTo("20");
        server.verify();
    }

    @Test
    void invalidAnimeIdMapsTo404() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime/99999999"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> provider.getAnimeById("99999999"))
                .isInstanceOf(ProviderException.class)
                .satisfies(ex -> assertThat(((ProviderException) ex).getUpstreamStatus()).isEqualTo(404));
        server.verify();
    }

    @Test
    void relatedAnimeMapsRelationTypes() {
        server.expect(request -> {
                    assertThat(request.getURI().toString()).contains("/anime/20");
                    assertThat(request.getURI().toString()).contains("related_anime");
                })
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"id":20,"title":"Naruto","related_anime":[
                                {"node":{"id":21,"title":"Naruto Shippuden","media_type":"tv","genres":[]},
                                "relation_type":"sequel","relation_type_formatted":"Sequel"}]}""",
                                MediaType.APPLICATION_JSON));

        List<RelatedAnimeDto> related = provider.getRelatedAnime("20");

        assertThat(related).hasSize(1);
        assertThat(related.get(0).relationType()).isEqualTo("SEQUEL");
        assertThat(related.get(0).anime().title()).isEqualTo("Naruto Shippuden");
        server.verify();
    }

    @Test
    void relatedAnimePreservesEverySourceRelationType() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime/20"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"id":20,"title":"Naruto","related_anime":[
                                {"node":{"id":21,"title":"Naruto Shippuden","main_picture":{"medium":"m1.jpg","large":"l1.jpg"}},
                                "relation_type":"sequel","relation_type_formatted":"Sequel"},
                                {"node":{"id":19,"title":"Naruto Special","main_picture":{"medium":"m2.jpg","large":"l2.jpg"}},
                                "relation_type":"side_story","relation_type_formatted":"Side story"},
                                {"node":{"id":22,"title":"Naruto Spin-off","main_picture":{"medium":"m3.jpg","large":"l3.jpg"}},
                                "relation_type":"spin_off","relation_type_formatted":"Spin-off"},
                                {"node":null,"relation_type":"prequel","relation_type_formatted":"Prequel"}]}""",
                                MediaType.APPLICATION_JSON));

        List<RelatedAnimeDto> related = provider.getRelatedAnime("20");

        // Null nodes are skipped; labels come straight from the MAL response.
        assertThat(related).hasSize(3);
        assertThat(related.stream().map(RelatedAnimeDto::relationType).toList())
                .containsExactly("SEQUEL", "SIDE_STORY", "SPIN_OFF");
        assertThat(related.get(0).anime().externalId()).isEqualTo("21");
        assertThat(related.get(0).anime().provider()).isEqualTo("MAL");
        assertThat(related.get(0).anime().coverImage()).isEqualTo("l1.jpg");
        server.verify();
    }

    @Test
    void missingRelationshipsReturnEmptyCollection() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime/20"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"id":20,"title":"Naruto"}""", MediaType.APPLICATION_JSON));

        assertThat(provider.getRelatedAnime("20")).isEmpty();
        server.verify();
    }

    @Test
    void nullRelationshipsReturnEmptyCollection() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime/20"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"id":20,"title":"Naruto","related_anime":null}""",
                                MediaType.APPLICATION_JSON));

        assertThat(provider.getRelatedAnime("20")).isEmpty();
        server.verify();
    }

    @Test
    void relatedAnimeServerErrorSurfacesAsProviderException() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime/20"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withServerError());

        assertThatThrownBy(() -> provider.getRelatedAnime("20"))
                .isInstanceOf(ProviderException.class);
        server.verify();
    }

    @Test
    void rankingReturnsItems() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("ranking_type=all"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess("""
                                {"data":[{"node":{"id":1,"title":"Top Anime","genres":[]},"ranking":{"rank":1}}],
                                "paging":{}}""", MediaType.APPLICATION_JSON));

        AnimeSearchResponse response = provider.getRanking("all", 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("Top Anime");
        server.verify();
    }

    @Test
    void missingClientIdFailsWithClearMessage() {
        MalAnimeProvider unconfigured =
                new MalAnimeProvider(new RestTemplate(), "https://api.myanimelist.net/v2", "");
        assertThatThrownBy(() -> unconfigured.searchAnime("naruto", 0, 10))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("MAL_CLIENT_ID");
        assertThatThrownBy(() -> unconfigured.getAnimeById("20"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("MAL_CLIENT_ID");
        assertThatThrownBy(() -> unconfigured.getRelatedAnime("20"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("MAL_CLIENT_ID");
        assertThatThrownBy(() -> unconfigured.getRanking("all", 0, 10))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("MAL_CLIENT_ID");
    }

    @Test
    void providerErrorSurfacesAsProviderException() {
        server.expect(request -> assertThat(request.getURI().toString()).contains("/anime?q=x"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withServerError());

        assertThatThrownBy(() -> provider.searchAnime("xyz", 0, 10))
                .isInstanceOf(ProviderException.class);
        server.verify();
    }
}
