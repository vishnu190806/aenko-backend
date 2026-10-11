package com.project.aenko.guide.dto;

import java.util.List;

public record WatchOrderDto(Long id, Long franchiseId, String name, String description, List<WatchOrderStepDto> steps) {
}
