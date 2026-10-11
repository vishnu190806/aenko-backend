package com.project.aenko.library.service;

import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.anime.service.AnimeService;
import com.project.aenko.common.exception.DuplicateResourceException;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.library.dto.LibraryRequest;
import com.project.aenko.library.dto.LibraryResponse;
import com.project.aenko.library.entity.UserMedia;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.repository.UserMediaRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {

    private final UserMediaRepository userMediaRepository;
    private final MediaRepository mediaRepository;
    private final AnimeService animeService;
    private final ActivityService activityService;

    public LibraryService(UserMediaRepository userMediaRepository, MediaRepository mediaRepository,
            AnimeService animeService, ActivityService activityService) {
        this.userMediaRepository = userMediaRepository;
        this.mediaRepository = mediaRepository;
        this.animeService = animeService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public Page<LibraryResponse> list(Long userId, LibraryStatus status, Pageable pageable) {
        Page<UserMedia> page = status == null
                ? userMediaRepository.findByUserId(userId, pageable)
                : userMediaRepository.findByUserIdAndStatus(userId, status, pageable);
        return page.map(entry -> LibraryResponse.from(entry, requireMedia(entry.getMediaId())));
    }

    @Transactional
    public LibraryResponse add(Long userId, LibraryRequest request) {
        Long mediaId = resolveMediaId(request);
        if (userMediaRepository.findByUserIdAndMediaId(userId, mediaId).isPresent()) {
            throw new DuplicateResourceException("Anime is already in your library");
        }
        UserMedia entry = new UserMedia();
        entry.setUserId(userId);
        entry.setMediaId(mediaId);
        entry.setStatus(request.status() != null ? request.status() : LibraryStatus.PLANNED);
        entry.setRating(request.rating());
        entry.setNotes(request.notes());
        applyTimestamps(entry);
        entry = userMediaRepository.save(entry);
        activityService.record(userId, "ADDED_ANIME", mediaId, entry.getStatus().name());
        return LibraryResponse.from(entry, requireMedia(mediaId));
    }

    @Transactional
    public LibraryResponse update(Long userId, Long mediaId, LibraryRequest request) {
        UserMedia entry = ownedEntry(userId, mediaId);
        if (request.status() != null) {
            entry.setStatus(request.status());
        }
        if (request.rating() != null) {
            entry.setRating(request.rating());
        }
        if (request.notes() != null) {
            entry.setNotes(request.notes());
        }
        applyTimestamps(entry);
        return LibraryResponse.from(userMediaRepository.save(entry), requireMedia(mediaId));
    }

    @Transactional
    public void remove(Long userId, Long mediaId) {
        UserMedia entry = ownedEntry(userId, mediaId);
        userMediaRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public UserMedia ownedEntry(Long userId, Long mediaId) {
        return userMediaRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found in your library"));
    }

    private Long resolveMediaId(LibraryRequest request) {
        if (request.mediaId() != null) {
            if (!mediaRepository.existsById(request.mediaId())) {
                throw new ResourceNotFoundException("Anime not found");
            }
            return request.mediaId();
        }
        if (request.externalId() == null || request.externalId().isBlank()) {
            throw new IllegalArgumentException("Provide mediaId or provider externalId");
        }
        Media media = animeService.getOrImport(request.provider(), request.externalId());
        return media.getId();
    }

    private Media requireMedia(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime not found"));
    }

    static void applyTimestamps(UserMedia entry) {
        if (entry.getStatus() == LibraryStatus.WATCHING && entry.getStartedAt() == null) {
            entry.setStartedAt(Instant.now());
        }
        if (entry.getStatus() == LibraryStatus.COMPLETED && entry.getCompletedAt() == null) {
            entry.setCompletedAt(Instant.now());
        }
        if (entry.getStatus() != LibraryStatus.COMPLETED) {
            entry.setCompletedAt(null);
        }
    }
}
