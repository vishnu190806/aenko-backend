package com.project.aenko.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
