package com.project.aenko.progress.controller;

import com.project.aenko.common.config.AppConfig;
import com.project.aenko.progress.dto.ProgressResponse;
import com.project.aenko.progress.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping("/{mediaId}/episode/{episodeNumber}")
    public ResponseEntity<ProgressResponse> markWatched(@PathVariable Long mediaId,
            @PathVariable int episodeNumber) {
        return ResponseEntity.ok(progressService.markWatched(AppConfig.currentUserId(), mediaId, episodeNumber));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ProgressResponse> view(@PathVariable Long mediaId) {
        return ResponseEntity.ok(progressService.view(AppConfig.currentUserId(), mediaId));
    }

    @DeleteMapping("/{mediaId}/episode/{episodeNumber}")
    public ResponseEntity<Void> remove(@PathVariable Long mediaId, @PathVariable int episodeNumber) {
        progressService.removeEpisode(AppConfig.currentUserId(), mediaId, episodeNumber);
        return ResponseEntity.noContent().build();
    }
}
