package com.project.aenko.progress.service;

import com.project.aenko.achievement.service.AchievementService;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.exception.DuplicateResourceException;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.guild.service.GuildService;
import com.project.aenko.library.entity.UserMedia;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.repository.UserMediaRepository;
import com.project.aenko.library.service.LibraryService;
import com.project.aenko.progress.dto.ProgressResponse;
import com.project.aenko.progress.entity.EpisodeProgress;
import com.project.aenko.progress.repository.EpisodeProgressRepository;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.quest.service.QuestService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Episode tracking. Every watch is verified server-side and then fans out to
 * XP, quests, achievements, guild contribution and activity — in that order.
 */
@Service
public class ProgressService {

    private final EpisodeProgressRepository episodeProgressRepository;
    private final UserMediaRepository userMediaRepository;
    private final MediaRepository mediaRepository;
    private final LibraryService libraryService;
    private final ProgressionService progressionService;
    private final QuestService questService;
    private final AchievementService achievementService;
    private final GuildService guildService;
    private final ActivityService activityService;

    public ProgressService(EpisodeProgressRepository episodeProgressRepository,
            UserMediaRepository userMediaRepository, MediaRepository mediaRepository,
            LibraryService libraryService, ProgressionService progressionService,
            QuestService questService, AchievementService achievementService,
            GuildService guildService, ActivityService activityService) {
        this.episodeProgressRepository = episodeProgressRepository;
        this.userMediaRepository = userMediaRepository;
        this.mediaRepository = mediaRepository;
        this.libraryService = libraryService;
        this.progressionService = progressionService;
        this.questService = questService;
        this.achievementService = achievementService;
        this.guildService = guildService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public ProgressResponse view(Long userId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found"));
        List<Integer> episodes = episodeProgressRepository.findByUserIdAndMediaIdOrderByEpisodeNumberAsc(userId, mediaId)
                .stream().map(EpisodeProgress::getEpisodeNumber).toList();
        return new ProgressResponse(mediaId, episodes.size(), media.getEpisodes(), episodes);
    }

    @Transactional
    public ProgressResponse markWatched(Long userId, Long mediaId, int episodeNumber) {
        if (episodeNumber <= 0) {
            throw new IllegalArgumentException("Episode number must be positive");
        }
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found"));
        if (media.getEpisodes() != null && episodeNumber > media.getEpisodes()) {
            throw new IllegalArgumentException(
                    "Episode " + episodeNumber + " exceeds the known total of " + media.getEpisodes());
        }
        if (episodeProgressRepository.existsByUserIdAndMediaIdAndEpisodeNumber(userId, mediaId, episodeNumber)) {
            throw new DuplicateResourceException("Episode already marked as watched");
        }

        EpisodeProgress progress = new EpisodeProgress();
        progress.setUserId(userId);
        progress.setMediaId(mediaId);
        progress.setEpisodeNumber(episodeNumber);
        episodeProgressRepository.save(progress);

        // Ensure the anime is in the user's library and move it to WATCHING.
        UserMedia entry = userMediaRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseGet(() -> {
                    UserMedia created = new UserMedia();
                    created.setUserId(userId);
                    created.setMediaId(mediaId);
                    created.setStatus(LibraryStatus.WATCHING);
                    created.setStartedAt(Instant.now());
                    return userMediaRepository.save(created);
                });
        if (entry.getStatus() == LibraryStatus.PLANNED || entry.getStatus() == LibraryStatus.ON_HOLD) {
            entry.setStatus(LibraryStatus.WATCHING);
            if (entry.getStartedAt() == null) {
                entry.setStartedAt(Instant.now());
            }
            userMediaRepository.save(entry);
        }

        // Auto-complete when all known episodes are watched.
        if (media.getEpisodes() != null && media.getEpisodes() > 0
                && episodeProgressRepository.countByUserIdAndMediaId(userId, mediaId) >= media.getEpisodes()
                && entry.getStatus() != LibraryStatus.COMPLETED) {
            entry.setStatus(LibraryStatus.COMPLETED);
            entry.setCompletedAt(Instant.now());
            userMediaRepository.save(entry);
            progressionService.awardXp(userId, "ANIME_COMPLETED", String.valueOf(mediaId),
                    ProgressionService.XP_PER_ANIME_COMPLETED);
            activityService.record(userId, "COMPLETED_ANIME", mediaId, media.getTitle());
        }

        progressionService.awardXp(userId, "EPISODE", mediaId + ":" + episodeNumber,
                ProgressionService.XP_PER_EPISODE);
        activityService.record(userId, "WATCHED_EPISODE", mediaId, "Episode " + episodeNumber);
        questService.refreshProgress(userId);
        achievementService.checkAchievements(userId);
        guildService.contributeEpisode(userId, mediaId);
        return view(userId, mediaId);
    }

    @Transactional
    public void removeEpisode(Long userId, Long mediaId, int episodeNumber) {
        EpisodeProgress progress = episodeProgressRepository
                .findByUserIdAndMediaIdAndEpisodeNumber(userId, mediaId, episodeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Episode progress not found"));
        episodeProgressRepository.delete(progress);
    }

    // Exposed for orchestration-free reads in other services.
    public UserMedia ownedEntry(Long userId, Long mediaId) {
        return libraryService.ownedEntry(userId, mediaId);
    }
}
