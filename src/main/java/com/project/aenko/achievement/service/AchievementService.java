package com.project.aenko.achievement.service;

import com.project.aenko.achievement.dto.AchievementDto;
import com.project.aenko.achievement.entity.Achievement;
import com.project.aenko.achievement.entity.UserAchievement;
import com.project.aenko.achievement.repository.AchievementRepository;
import com.project.aenko.achievement.repository.UserAchievementRepository;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.guild.repository.GuildMemberRepository;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.repository.UserMediaRepository;
import com.project.aenko.progress.repository.EpisodeProgressRepository;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.quest.entity.UserQuest.QuestStatus;
import com.project.aenko.quest.repository.UserQuestRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backend-owned achievement unlocks. There is no endpoint that unlocks an
 * achievement directly; this service evaluates rules after verified activity.
 */
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final EpisodeProgressRepository episodeProgressRepository;
    private final UserMediaRepository userMediaRepository;
    private final MediaRepository mediaRepository;
    private final UserQuestRepository userQuestRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final ProgressionService progressionService;
    private final ActivityService activityService;

    public AchievementService(AchievementRepository achievementRepository,
            UserAchievementRepository userAchievementRepository,
            EpisodeProgressRepository episodeProgressRepository,
            UserMediaRepository userMediaRepository,
            MediaRepository mediaRepository,
            UserQuestRepository userQuestRepository,
            GuildMemberRepository guildMemberRepository,
            ProgressionService progressionService,
            ActivityService activityService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.episodeProgressRepository = episodeProgressRepository;
        this.userMediaRepository = userMediaRepository;
        this.mediaRepository = mediaRepository;
        this.userQuestRepository = userQuestRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.progressionService = progressionService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> listAll() {
        return achievementRepository.findAll().stream()
                .map(AchievementDto::locked)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> list(Long userId) {
        Map<Long, UserAchievement> unlocked = userAchievementRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, ua -> ua));
        return achievementRepository.findAll().stream()
                .map(achievement -> unlocked.containsKey(achievement.getId())
                        ? AchievementDto.unlocked(achievement, unlocked.get(achievement.getId()).getUnlockedAt())
                        : AchievementDto.locked(achievement))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> myUnlocked(Long userId) {
        Map<Long, Achievement> all = achievementRepository.findAll().stream()
                .collect(Collectors.toMap(Achievement::getId, a -> a));
        return userAchievementRepository.findByUserId(userId).stream()
                .filter(ua -> all.containsKey(ua.getAchievementId()))
                .map(ua -> AchievementDto.unlocked(all.get(ua.getAchievementId()), ua.getUnlockedAt()))
                .toList();
    }

    @Transactional
    public void checkAchievements(Long userId) {
        Set<Long> unlocked = userAchievementRepository.findByUserId(userId).stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());
        for (Achievement achievement : achievementRepository.findAll()) {
            if (unlocked.contains(achievement.getId())) {
                continue;
            }
            if (currentValue(achievement.getRequirementType(), userId) >= achievement.getRequirementValue()) {
                UserAchievement userAchievement = new UserAchievement();
                userAchievement.setUserId(userId);
                userAchievement.setAchievementId(achievement.getId());
                userAchievementRepository.save(userAchievement);
                progressionService.awardXp(userId, "ACHIEVEMENT",
                        String.valueOf(achievement.getId()), achievement.getXpReward());
                activityService.record(userId, "UNLOCKED_ACHIEVEMENT",
                        achievement.getId(), achievement.getName());
            }
        }
    }

    private long currentValue(String requirementType, Long userId) {
        return switch (requirementType) {
            case "EPISODES_WATCHED" -> episodeProgressRepository.countByUserId(userId);
            case "ANIME_COMPLETED" -> userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED);
            case "GENRES_EXPLORED" -> distinctGenres(userId).size();
            case "QUESTS_COMPLETED" -> userQuestRepository.countByUserIdAndStatus(userId, QuestStatus.COMPLETED);
            case "GUILD_JOINED" -> guildMemberRepository.countByUserId(userId) > 0 ? 1 : 0;
            default -> 0;
        };
    }

    private Set<String> distinctGenres(Long userId) {
        Set<String> genres = new HashSet<>();
        for (var entry : userMediaRepository.findByUserId(userId)) {
            mediaRepository.findById(entry.getMediaId()).ifPresent(media -> {
                if (media.getGenres() != null && !media.getGenres().isBlank()) {
                    genres.addAll(Arrays.asList(media.getGenres().split(",")));
                }
            });
        }
        return genres;
    }
}
