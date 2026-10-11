package com.project.aenko.guild.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuildRequest(
        @NotBlank(message = "Guild name is required") @Size(max = 100, message = "Name must be under 100 characters") String name,
        @Size(max = 10, message = "Tag must be under 10 characters") String tag,
        @Size(max = 2000, message = "Description must be under 2000 characters") String description,
        String privacy) {
}
