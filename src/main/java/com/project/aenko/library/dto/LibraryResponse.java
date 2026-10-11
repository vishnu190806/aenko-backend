package com.project.aenko.library.dto;

import com.project.aenko.anime.entity.Media;
import com.project.aenko.library.entity.UserMedia;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record LibraryResponse(
        Long mediaId,
        String title,
        String englishTitle,
        String coverImage,
        Integer episodes,
        Double score,
        List<String> genres,
        String status,
        Integer rating,
        String notes,
        Instant startedAt,
        Instant completedAt) {

    public static LibraryResponse from(UserMedia entry, Media media) {
        List<String> genres = media.getGenres() == null || media.getGenres().isBlank()
                ? List.of()
                : Arrays.asList(media.getGenres().split(","));
        return new LibraryResponse(
                media.getId(), media.getTitle(), media.getEnglishTitle(), media.getCoverImage(),
                media.getEpisodes(), media.getScore(), genres, entry.getStatus().name(),
                entry.getRating(), entry.getNotes(), entry.getStartedAt(), entry.getCompletedAt());
    }
}
