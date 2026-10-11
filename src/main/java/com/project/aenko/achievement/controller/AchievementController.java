package com.project.aenko.achievement.controller;

import com.project.aenko.achievement.dto.AchievementDto;
import com.project.aenko.achievement.service.AchievementService;
import com.project.aenko.common.config.AppConfig;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<List<AchievementDto>> list() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.ok(achievementService.listAll());
        }
        return ResponseEntity.ok(achievementService.list(userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AchievementDto>> my() {
        return ResponseEntity.ok(achievementService.myUnlocked(AppConfig.currentUserId()));
    }

    private Long currentUserIdOrNull() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.project.aenko.common.security.AppUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
