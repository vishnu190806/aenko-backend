package com.project.aenko.provider.mal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Raw MyAnimeList API v2 JSON shapes. These classes are only used inside the
 * MAL provider and are never exposed to the rest of the application.
 */
public final class MalResponses {

    private MalResponses() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalPicture(String medium, String large) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalGenre(int id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalAlternativeTitles(String en, String ja, List<String> synonyms) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalNode(
            int id,
            String title,
            MalPicture main_picture,
            MalAlternativeTitles alternative_titles,
            String synopsis,
            Double mean,
            String media_type,
            String status,
            Integer num_episodes,
            List<MalGenre> genres,
            String rating,
            String source,
            String start_date,
            String end_date,
            List<MalRelatedEdge> related_anime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalRelatedEdge(MalNode node, String relation_type, String relation_type_formatted) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalSearchItem(MalNode node) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalRankingItem(MalNode node, MalRank ranking) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalRank(Integer rank) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalPaging(String previous, String next) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalSearchResponse(List<MalSearchItem> data, MalPaging paging) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MalRankingResponse(List<MalRankingItem> data, MalPaging paging) {
    }
}
