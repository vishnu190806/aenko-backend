package com.project.aenko.provider.mal;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.provider.mal.dto.MalResponses.MalNode;
import java.util.List;

/**
 * Converts raw MAL nodes into the canonical AENKO model.
 */
public final class MalMapper {

    private MalMapper() {
    }

    public static AnimeDto toCanonical(MalNode node) {
        if (node == null) {
            return null;
        }
        String englishTitle = node.alternative_titles() != null ? node.alternative_titles().en() : null;
        String cover = node.main_picture() != null ? node.main_picture().large() : null;
        if (cover == null && node.main_picture() != null) {
            cover = node.main_picture().medium();
        }
        List<String> genres = node.genres() == null ? List.of()
                : node.genres().stream().map(g -> g.name()).toList();
        return new AnimeDto(
                null,
                String.valueOf(node.id()),
                "MAL",
                node.title(),
                englishTitle,
                node.synopsis(),
                node.media_type(),
                node.status(),
                node.num_episodes(),
                node.mean(),
                cover,
                genres,
                "https://myanimelist.net/anime/" + node.id());
    }

    public static List<RelatedAnimeDto> toRelated(List<com.project.aenko.provider.mal.dto.MalResponses.MalRelatedEdge> edges) {
        if (edges == null) {
            return List.of();
        }
        return edges.stream()
                .filter(edge -> edge != null && edge.node() != null)
                .map(edge -> new RelatedAnimeDto(toCanonical(edge.node()),
                        edge.relation_type() != null ? edge.relation_type().toUpperCase() : "RELATED"))
                .toList();
    }
}
