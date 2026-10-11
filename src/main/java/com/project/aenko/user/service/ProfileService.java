package com.project.aenko.user.service;

import com.project.aenko.achievement.repository.UserAchievementRepository;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.dto.UserDto;
import com.project.aenko.auth.entity.User;
import com.project.aenko.auth.repository.UserRepository;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.guild.repository.GuildMemberRepository;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.repository.UserMediaRepository;
import com.project.aenko.progress.repository.EpisodeProgressRepository;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.quest.entity.UserQuest.QuestStatus;
import com.project.aenko.quest.repository.UserQuestRepository;
import com.project.aenko.user.dto.ProfileDto;
import com.project.aenko.user.dto.StatsDto;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AnimeVerse profile. Every number is derived from stored records —
 * nothing is kept as a mutable counter column.
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMediaRepository userMediaRepository;
    private final MediaRepository mediaRepository;
    private final EpisodeProgressRepository episodeProgressRepository;
    private final ProgressionService progressionService;
    private final UserAchievementRepository userAchievementRepository;
    private final UserQuestRepository userQuestRepository;
    private final GuildMemberRepository guildMemberRepository;

    public ProfileService(UserRepository userRepository, UserMediaRepository userMediaRepository,
            MediaRepository mediaRepository, EpisodeProgressRepository episodeProgressRepository,
            ProgressionService progressionService, UserAchievementRepository userAchievementRepository,
            UserQuestRepository userQuestRepository, GuildMemberRepository guildMemberRepository) {
        this.userRepository = userRepository;
        this.userMediaRepository = userMediaRepository;
        this.mediaRepository = mediaRepository;
        this.episodeProgressRepository = episodeProgressRepository;
        this.progressionService = progressionService;
        this.userAchievementRepository = userAchievementRepository;
        this.userQuestRepository = userQuestRepository;
        this.guildMemberRepository = guildMemberRepository;
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new ProfileDto(UserDto.from(user), stats(userId));
    }

    @Transactional(readOnly = true)
    public StatsDto stats(Long userId) {
        long totalXp = progressionService.totalXp(userId);
        Map<String, Long> genreCounts = genreCounts(userId);
        List<Map<String, Object>> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("genre", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .toList();
        return new StatsDto(
                userMediaRepository.countByUserId(userId),
                userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.WATCHING),
                userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED),
                userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.PLANNED),
                userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.ON_HOLD),
                userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.DROPPED),
                episodeProgressRepository.countByUserId(userId),
                totalXp,
                ProgressionService.levelFor(totalXp),
                userAchievementRepository.countByUserId(userId),
                userQuestRepository.countByUserIdAndStatus(userId, QuestStatus.COMPLETED),
                guildMemberRepository.countByUserId(userId),
                streakDays(userId),
                topGenres);
    }

    private Map<String, Long> genreCounts(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        for (var entry : userMediaRepository.findByUserId(userId)) {
            mediaRepository.findById(entry.getMediaId()).ifPresent(media -> {
                if (media.getGenres() != null && !media.getGenres().isBlank()) {
                    Arrays.stream(media.getGenres().split(","))
                            .map(String::trim)
                            .filter(g -> !g.isEmpty())
                            .forEach(genre -> counts.merge(genre, 1L, Long::sum));
                }
            });
        }
        return counts;
    }

    private int streakDays(Long userId) {
        Set<LocalDate> days = episodeProgressRepository.findWatchedAtByUserId(userId).stream()
                .map(instant -> LocalDate.ofInstant(instant, ZoneOffset.UTC))
                .collect(Collectors.toSet());
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
