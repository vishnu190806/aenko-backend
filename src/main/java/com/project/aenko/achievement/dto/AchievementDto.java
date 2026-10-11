package com.project.aenko.achievement.dto;

import com.project.aenko.achievement.entity.Achievement;
import java.time.Instant;

public record AchievementDto(
        Long id,
        String name,
        String description,
        int xpReward,
        String requirementType,
        int requirementValue,
        boolean unlocked,
        Instant unlockedAt) {

    public static AchievementDto locked(Achievement achievement) {
        return new AchievementDto(achievement.getId(), achievement.getName(), achievement.getDescription(),
                achievement.getXpReward(), achievement.getRequirementType(), achievement.getRequirementValue(),
                false, null);
    }

    public static AchievementDto unlocked(Achievement achievement, Instant unlockedAt) {
        return new AchievementDto(achievement.getId(), achievement.getName(), achievement.getDescription(),
                achievement.getXpReward(), achievement.getRequirementType(), achievement.getRequirementValue(),
                true, unlockedAt);
    }
}
