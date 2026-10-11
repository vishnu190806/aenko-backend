package com.project.aenko.achievement.repository;

import com.project.aenko.achievement.entity.UserAchievement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    List<UserAchievement> findByUserId(Long userId);

    long countByUserId(Long userId);
}
