package com.project.aenko.quest.service;

import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.exception.DuplicateResourceException;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.guide.repository.FranchiseMediaRepository;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.repository.UserMediaRepository;
import com.project.aenko.progress.repository.EpisodeProgressRepository;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.quest.dto.QuestDto;
import com.project.aenko.quest.entity.Quest;
import com.project.aenko.quest.entity.UserQuest;
import com.project.aenko.quest.entity.UserQuest.QuestStatus;
import com.project.aenko.quest.repository.QuestRepository;
import com.project.aenko.quest.repository.UserQuestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final EpisodeProgressRepository episodeProgressRepository;
    private final UserMediaRepository userMediaRepository;
    private final MediaRepository mediaRepository;
    private final FranchiseMediaRepository franchiseMediaRepository;
    private final ProgressionService progressionService;
    private final ActivityService activityService;

    public QuestService(QuestRepository questRepository, UserQuestRepository userQuestRepository,
            EpisodeProgressRepository episodeProgressRepository, UserMediaRepository userMediaRepository,
            MediaRepository mediaRepository, FranchiseMediaRepository franchiseMediaRepository,
            ProgressionService progressionService, ActivityService activityService) {
        this.questRepository = questRepository;
        this.userQuestRepository = userQuestRepository;
        this.episodeProgressRepository = episodeProgressRepository;
        this.userMediaRepository = userMediaRepository;
        this.mediaRepository = mediaRepository;
        this.franchiseMediaRepository = franchiseMediaRepository;
        this.progressionService = progressionService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<QuestDto> listAll() {
        return questRepository.findAll().stream().map(QuestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestDto> myQuests(Long userId) {
        Map<Long, Quest> quests = questRepository.findAll().stream()
                .collect(Collectors.toMap(Quest::getId, q -> q));
        return userQuestRepository.findByUserId(userId).stream()
                .filter(uq -> quests.containsKey(uq.getQuestId()))
                .map(uq -> QuestDto.from(quests.get(uq.getQuestId()), uq))
                .toList();
    }

    @Transactional
    public QuestDto start(Long userId, Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        if (userQuestRepository.findByUserIdAndQuestId(userId, questId).isPresent()) {
            throw new DuplicateResourceException("Quest already started");
        }
        UserQuest userQuest = new UserQuest();
        userQuest.setUserId(userId);
        userQuest.setQuestId(questId);
        userQuest.setProgress(evaluate(quest, userId));
        userQuest = userQuestRepository.save(userQuest);
        activityService.record(userId, "STARTED_QUEST", questId, quest.getName());
        maybeComplete(userId, quest, userQuest);
        return QuestDto.from(quest, userQuestRepository.findById(userQuest.getId()).orElse(userQuest));
    }

    @Transactional
    public QuestDto complete(Long userId, Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        UserQuest userQuest = userQuestRepository.findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not started"));
        userQuest.setProgress(evaluate(quest, userId));
        maybeComplete(userId, quest, userQuest);
        userQuest = userQuestRepository.save(userQuest);
        if (userQuest.getStatus() != QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("Quest requirements are not met yet");
        }
        return QuestDto.from(quest, userQuest);
    }

    /**
     * Re-evaluates every started quest after verified user activity.
     */
    @Transactional
    public void refreshProgress(Long userId) {
        List<UserQuest> started = userQuestRepository.findByUserIdAndStatus(userId, QuestStatus.STARTED);
        if (started.isEmpty()) {
            return;
        }
        Map<Long, Quest> quests = questRepository.findAll().stream()
                .collect(Collectors.toMap(Quest::getId, q -> q));
        for (UserQuest userQuest : started) {
            Quest quest = quests.get(userQuest.getQuestId());
            if (quest == null) {
                continue;
            }
            userQuest.setProgress(evaluate(quest, userId));
            maybeComplete(userId, quest, userQuest);
            userQuestRepository.save(userQuest);
        }
    }

    private void maybeComplete(Long userId, Quest quest, UserQuest userQuest) {
        if (userQuest.getStatus() == QuestStatus.COMPLETED) {
            return;
        }
        if (userQuest.getProgress() >= quest.getTargetValue()) {
            userQuest.setStatus(QuestStatus.COMPLETED);
            userQuest.setCompletedAt(Instant.now());
            progressionService.awardXp(userId, "QUEST", String.valueOf(quest.getId()), quest.getXpReward());
            activityService.record(userId, "COMPLETED_QUEST", quest.getId(), quest.getName());
        }
    }

    int evaluate(Quest quest, Long userId) {
        return switch (quest.getType()) {
            case EPISODE_COUNT -> (int) episodeProgressRepository.countByUserId(userId);
            case ANIME_COUNT -> (int) userMediaRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED);
            case GENRE_COUNT -> distinctGenres(userId).size();
            case STREAK -> currentStreakDays(userId);
            case FRANCHISE_COMPLETION -> completedFranchises(userId);
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

    private int currentStreakDays(Long userId) {
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

    private int completedFranchises(Long userId) {
        Set<Long> completedMedia = userMediaRepository.findByUserId(userId).stream()
                .filter(e -> e.getStatus() == LibraryStatus.COMPLETED)
                .map(e -> e.getMediaId())
                .collect(Collectors.toSet());
        if (completedMedia.isEmpty()) {
            return 0;
        }
        Map<Long, List<Long>> byFranchise = franchiseMediaRepository.findAll().stream()
                .collect(Collectors.groupingBy(fm -> fm.getFranchiseId(),
                        Collectors.mapping(fm -> fm.getMediaId(), Collectors.toList())));
        int completed = 0;
        for (List<Long> mediaIds : byFranchise.values()) {
            if (!mediaIds.isEmpty() && completedMedia.containsAll(mediaIds)) {
                completed++;
            }
        }
        return completed;
    }
}
