package com.project.aenko.auth.dto;

import com.project.aenko.auth.entity.User;
import java.time.Instant;

public record UserDto(Long id, String username, String email, String role, Instant createdAt) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getCreatedAt());
    }
}
