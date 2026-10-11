package com.project.aenko.user.dto;

import java.util.List;
import java.util.Map;

public record StatsDto(
        long totalAnime,
        long watching,
        long completed,
        long planned,
        long onHold,
        long dropped,
        long episodesWatched,
        long totalXp,
        int level,
        long achievementsCount,
        long questsCompleted,
        long guildCount,
        int currentStreakDays,
        List<Map<String, Object>> topGenres) {
}
