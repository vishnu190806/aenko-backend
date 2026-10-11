package com.project.aenko.progress.repository;

import com.project.aenko.progress.entity.EpisodeProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EpisodeProgressRepository extends JpaRepository<EpisodeProgress, Long> {
    List<EpisodeProgress> findByUserIdAndMediaIdOrderByEpisodeNumberAsc(Long userId, Long mediaId);

    Optional<EpisodeProgress> findByUserIdAndMediaIdAndEpisodeNumber(Long userId, Long mediaId, int episodeNumber);

    long countByUserId(Long userId);

    long countByUserIdAndMediaId(Long userId, Long mediaId);

    boolean existsByUserIdAndMediaIdAndEpisodeNumber(Long userId, Long mediaId, int episodeNumber);

    @Query("select e.watchedAt from EpisodeProgress e where e.userId = :userId order by e.watchedAt desc")
    List<java.time.Instant> findWatchedAtByUserId(Long userId);
}
