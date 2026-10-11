package com.project.aenko.user.controller;

import com.project.aenko.common.config.AppConfig;
import com.project.aenko.user.dto.ProfileDto;
import com.project.aenko.user.dto.StatsDto;
import com.project.aenko.user.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileDto> profile() {
        return ResponseEntity.ok(profileService.profile(AppConfig.currentUserId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDto> stats() {
        return ResponseEntity.ok(profileService.stats(AppConfig.currentUserId()));
    }
}
