package com.project.aenko.quest.dto;

import com.project.aenko.quest.entity.Quest;
import com.project.aenko.quest.entity.UserQuest;
import java.time.Instant;

public record QuestDto(
        Long id,
        String name,
        String description,
        String type,
        int targetValue,
        int xpReward,
        String difficulty,
        int progress,
        String status,
        Instant completedAt) {

    public static QuestDto from(Quest quest) {
        return new QuestDto(quest.getId(), quest.getName(), quest.getDescription(),
                quest.getType().name(), quest.getTargetValue(), quest.getXpReward(),
                quest.getDifficulty(), 0, null, null);
    }

    public static QuestDto from(Quest quest, UserQuest userQuest) {
        return new QuestDto(quest.getId(), quest.getName(), quest.getDescription(),
                quest.getType().name(), quest.getTargetValue(), quest.getXpReward(),
                quest.getDifficulty(), userQuest.getProgress(),
                userQuest.getStatus().name(), userQuest.getCompletedAt());
    }
}
