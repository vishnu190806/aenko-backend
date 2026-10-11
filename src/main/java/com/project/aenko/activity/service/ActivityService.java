package com.project.aenko.activity.service;

import com.project.aenko.activity.dto.ActivityDto;
import com.project.aenko.activity.entity.Activity;
import com.project.aenko.activity.repository.ActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Transactional
    public void record(Long userId, String activityType, Long referenceId, String metadata) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setActivityType(activityType);
        activity.setReferenceId(referenceId);
        activity.setMetadata(metadata);
        activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<ActivityDto> recent(Long userId, Pageable pageable) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(ActivityDto::from);
    }
}
