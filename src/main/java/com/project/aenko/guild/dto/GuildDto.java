package com.project.aenko.guild.dto;

import com.project.aenko.guild.entity.Guild;
import java.time.Instant;

public record GuildDto(
        Long id,
        String name,
        String tag,
        String description,
        Long ownerId,
        String privacy,
        long memberCount,
        long totalXp,
        Instant createdAt) {

    public static GuildDto from(Guild guild, long memberCount, long totalXp) {
        return new GuildDto(guild.getId(), guild.getName(), guild.getTag(), guild.getDescription(),
                guild.getOwnerId(), guild.getPrivacy(), memberCount, totalXp, guild.getCreatedAt());
    }
}
