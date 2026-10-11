package com.project.aenko.guild.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GuildQuestRequest(
        @NotBlank(message = "Quest name is required") String name,
        String description,
        @Min(value = 1, message = "Target must be positive") int targetValue,
        @Min(value = 0, message = "XP reward must not be negative") int xpReward) {
}
