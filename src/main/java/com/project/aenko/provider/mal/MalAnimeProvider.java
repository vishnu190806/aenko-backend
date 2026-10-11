package com.project.aenko.provider.mal;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.common.exception.ProviderException;
import com.project.aenko.provider.api.AnimeProvider;
import com.project.aenko.provider.mal.dto.MalResponses.MalNode;
import com.project.aenko.provider.mal.dto.MalResponses.MalRankingResponse;
import com.project.aenko.provider.mal.dto.MalResponses.MalSearchResponse;
import java.util.List;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * MyAnimeList implementation of {@link AnimeProvider}.
 * This is the ONLY class that knows MAL-specific API details
 * (base URL, {@code X-MAL-CLIENT-ID} header, field list, JSON shapes).
 */
@Component("malAnimeProvider")
public class MalAnimeProvider implements AnimeProvider {

    private static final Logger log = LoggerFactory.getLogger(MalAnimeProvider.class);

    static final String FIELDS = String.join(",",
            "id", "title", "main_picture", "alternative_titles", "synopsis", "mean",
            "media_type", "status", "genres", "num_episodes", "start_season", "source",
            "average_episode_duration", "rating", "studios", "related_anime",
            "start_date", "end_date", "background", "nsfw", "num_list_users");

    /**
     * Field selector used for the relations lookup. {@code related_anime} is
     * the relationship field supported by the MAL API v2 anime-details
     * endpoint; the nested nodes carry the related titles' id/title/picture.
     */
    static final String RELATED_FIELDS = "related_anime";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String clientId;

    public MalAnimeProvider(RestTemplate restTemplate,
            @Value("${mal.base-url:https://api.myanimelist.net/v2}") String baseUrl,
            @Value("${mal.client-id:}") String clientId) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    @Override
    public String providerName() {
        return "MAL";
    }

    private HttpHeaders headers() {
        if (clientId.isEmpty()) {
            throw new ProviderException("MAL client ID is not configured. Set MAL_CLIENT_ID to enable anime search.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MAL-CLIENT-ID", clientId);
        return headers;
    }

    /**
     * Builds an encoded {@link URI} so MAL field selectors containing
     * {@code {braces}} are sent literally instead of being treated as
     * URI-template variables.
     */
    private static URI uri(String baseUrl, String path, Object... queryValues) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path);
        for (int i = 0; i < queryValues.length; i += 2) {
            builder.queryParam(String.valueOf(queryValues[i]), queryValues[i + 1]);
        }
        return builder.encode().build().toUri();
    }

    @Override
    public AnimeSearchResponse searchAnime(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        int limit = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * limit;
        URI url = uri(baseUrl, "/anime", "q", query, "limit", limit, "offset", offset, "fields", FIELDS);
        log.debug("MAL search: q={} limit={} offset={}", query, limit, offset);
        try {
            ResponseEntity<MalSearchResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), MalSearchResponse.class);
            MalSearchResponse body = response.getBody();
            List<AnimeDto> items = body == null || body.data() == null ? List.of()
                    : body.data().stream()
                            .filter(item -> item != null && item.node() != null)
                            .map(item -> MalMapper.toCanonical(item.node()))
                            .toList();
            boolean hasNext = body != null && body.paging() != null && body.paging().next() != null;
            return new AnimeSearchResponse(items, page, limit, hasNext);
        } catch (HttpClientErrorException ex) {
            throw new ProviderException("MAL search failed: " + ex.getStatusCode(), ex.getStatusCode().value());
        } catch (ProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("MAL search failed", ex);
            throw new ProviderException("MAL search failed: provider unavailable", ex);
        }
    }

    @Override
    public AnimeDto getAnimeById(String externalId) {
        return MalMapper.toCanonical(fetchNode(externalId, FIELDS));
    }

    private MalNode fetchNode(String externalId, String fields) {
        URI url = uri(baseUrl, "/anime/" + externalId, "fields", fields);
        try {
            ResponseEntity<MalNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), MalNode.class);
            MalNode node = response.getBody();
            if (node == null) {
                throw new ProviderException("Anime not found", 404);
            }
            return node;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProviderException("Anime not found", 404);
        } catch (HttpClientErrorException ex) {
            throw new ProviderException("MAL request failed: " + ex.getStatusCode(), ex.getStatusCode().value());
        } catch (ProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("MAL detail failed for id={}", externalId, ex);
            throw new ProviderException("MAL request failed: provider unavailable", ex);
        }
    }

    @Override
    public List<RelatedAnimeDto> getRelatedAnime(String externalId) {
        MalNode node = fetchNode(externalId, RELATED_FIELDS);
        if (node.related_anime() == null) {
            return List.of();
        }
        return MalMapper.toRelated(node.related_anime());
    }

    @Override
    public AnimeSearchResponse getRanking(String rankingType, int page, int size) {
        String type = (rankingType == null || rankingType.isBlank()) ? "all" : rankingType;
        int limit = Math.min(Math.max(size, 1), 100);
        int offset = Math.max(page, 0) * limit;
        URI url = uri(baseUrl, "/anime/ranking",
                "ranking_type", type, "limit", limit, "offset", offset, "fields", FIELDS);
        try {
            ResponseEntity<MalRankingResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), MalRankingResponse.class);
            MalRankingResponse body = response.getBody();
            List<AnimeDto> items = body == null || body.data() == null ? List.of()
                    : body.data().stream()
                            .filter(item -> item != null && item.node() != null)
                            .map(item -> MalMapper.toCanonical(item.node()))
                            .toList();
            boolean hasNext = body != null && body.paging() != null && body.paging().next() != null;
            return new AnimeSearchResponse(items, page, limit, hasNext);
        } catch (HttpClientErrorException ex) {
            throw new ProviderException("MAL ranking failed: " + ex.getStatusCode(), ex.getStatusCode().value());
        } catch (ProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("MAL ranking failed", ex);
            throw new ProviderException("MAL ranking failed: provider unavailable", ex);
        }
    }
}
