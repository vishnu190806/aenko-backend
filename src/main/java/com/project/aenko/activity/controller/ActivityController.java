package com.project.aenko.activity.controller;

import com.project.aenko.activity.dto.ActivityDto;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.common.config.AppConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<Page<ActivityDto>> myActivity(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(activityService.recent(AppConfig.currentUserId(), pageable));
    }
}
