package com.project.aenko.guide.dto;

public record WatchOrderStepDto(int sequenceNumber, Long mediaId, String title, String stepType, String notes,
        String externalId, String externalUrl) {
}
