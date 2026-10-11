package com.project.aenko.user.dto;

import com.project.aenko.auth.dto.UserDto;

public record ProfileDto(UserDto user, StatsDto stats) {
}
