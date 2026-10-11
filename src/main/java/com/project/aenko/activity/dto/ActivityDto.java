package com.project.aenko.activity.dto;

import com.project.aenko.activity.entity.Activity;
import java.time.Instant;

public record ActivityDto(Long id, String activityType, Long referenceId, String metadata, Instant createdAt) {
    public static ActivityDto from(Activity activity) {
        return new ActivityDto(activity.getId(), activity.getActivityType(),
                activity.getReferenceId(), activity.getMetadata(), activity.getCreatedAt());
    }
}
