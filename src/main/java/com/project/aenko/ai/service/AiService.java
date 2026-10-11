package com.project.aenko.ai.service;

import com.project.aenko.activity.dto.ActivityDto;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.ai.dto.InsightResponse;
import com.project.aenko.ai.entity.AiInsight;
import com.project.aenko.ai.provider.AiProvider;
import com.project.aenko.ai.repository.AiInsightRepository;
import com.project.aenko.user.dto.StatsDto;
import com.project.aenko.user.service.ProfileService;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advisory-only personalization. This service only reads user statistics and
 * stores the generated text — it never modifies XP, quests, achievements,
 * guilds, library entries or roles.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final ProfileService profileService;
    private final ActivityService activityService;
    private final AiProvider aiProvider;
    private final AiInsightRepository aiInsightRepository;

    public AiService(ProfileService profileService, ActivityService activityService,
            AiProvider aiProvider, AiInsightRepository aiInsightRepository) {
        this.profileService = profileService;
        this.activityService = activityService;
        this.aiProvider = aiProvider;
        this.aiInsightRepository = aiInsightRepository;
    }

    @Transactional
    public InsightResponse insight(Long userId) {
        StatsDto stats = profileService.stats(userId);
        String context = buildContext(stats, userId);
        if (!aiProvider.isAvailable()) {
            return save(userId, fallback(stats), false);
        }
        try {
            return save(userId, aiProvider.generateInsight(context), true);
        } catch (Exception ex) {
            log.warn("AI insight failed, returning fallback", ex);
            return save(userId, fallback(stats), false);
        }
    }

    private InsightResponse save(Long userId, String content, boolean personalized) {
        AiInsight insight = new AiInsight();
        insight.setUserId(userId);
        insight.setType("JOURNEY_INSIGHT");
        insight.setContent(content);
        aiInsightRepository.save(insight);
        return new InsightResponse("JOURNEY_INSIGHT", content, personalized);
    }

    private String buildContext(StatsDto stats, Long userId) {
        String recent = activityService.recent(userId, PageRequest.of(0, 5)).stream()
                .map(ActivityDto::activityType)
                .collect(Collectors.joining(", "));
        String genres = stats.topGenres().stream()
                .map(g -> String.valueOf(g.get("genre")))
                .collect(Collectors.joining(", "));
        return "Anime stats: total=" + stats.totalAnime()
                + ", watching=" + stats.watching()
                + ", completed=" + stats.completed()
                + ", episodesWatched=" + stats.episodesWatched()
                + ", level=" + stats.level()
                + ", questsCompleted=" + stats.questsCompleted()
                + ", streakDays=" + stats.currentStreakDays()
                + ", topGenres=[" + genres + "]"
                + ", recentActivity=[" + recent + "]";
    }

    String fallback(StatsDto stats) {
        if (stats.totalAnime() == 0) {
            return "Your anime journey is just beginning. Add your first anime to the library "
                    + "and mark an episode as watched to start earning XP and unlocking quests.";
        }
        String genre = stats.topGenres().isEmpty()
                ? "a new genre"
                : String.valueOf(stats.topGenres().get(0).get("genre"));
        return "Personalized AI insight is currently unavailable, but your journey looks good: "
                + "you have completed " + stats.completed() + " anime and watched "
                + stats.episodesWatched() + " episodes at level " + stats.level() + ". "
                + "Keep exploring " + genre + " titles and finish an in-progress anime "
                + "to keep your streak alive.";
    }
}
