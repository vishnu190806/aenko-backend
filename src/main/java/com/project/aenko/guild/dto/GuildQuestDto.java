package com.project.aenko.guild.dto;

import com.project.aenko.guild.entity.GuildQuest;
import java.time.Instant;

public record GuildQuestDto(
        Long id,
        Long guildId,
        String name,
        String description,
        int targetValue,
        int currentValue,
        int xpReward,
        String status,
        Instant endDate) {

    public static GuildQuestDto from(GuildQuest quest, int currentValue) {
        return new GuildQuestDto(quest.getId(), quest.getGuildId(), quest.getName(),
                quest.getDescription(), quest.getTargetValue(), currentValue,
                quest.getXpReward(), quest.getStatus(), quest.getEndDate());
    }
}
