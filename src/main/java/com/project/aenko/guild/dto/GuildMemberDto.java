package com.project.aenko.guild.dto;

import java.time.Instant;

public record GuildMemberDto(Long userId, String username, String role, Instant joinedAt) {
}
