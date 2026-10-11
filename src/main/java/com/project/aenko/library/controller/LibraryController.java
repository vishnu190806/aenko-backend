package com.project.aenko.library.controller;

import com.project.aenko.library.dto.LibraryRequest;
import com.project.aenko.library.dto.LibraryResponse;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import com.project.aenko.library.service.LibraryService;
import com.project.aenko.common.config.AppConfig;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public ResponseEntity<Page<LibraryResponse>> list(
            @RequestParam(value = "status", required = false) LibraryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(libraryService.list(AppConfig.currentUserId(), status, pageable));
    }

    @PostMapping
    public ResponseEntity<LibraryResponse> add(@Valid @RequestBody LibraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(libraryService.add(AppConfig.currentUserId(), request));
    }

    @PutMapping("/{mediaId}")
    public ResponseEntity<LibraryResponse> update(@PathVariable Long mediaId,
            @Valid @RequestBody LibraryRequest request) {
        return ResponseEntity.ok(libraryService.update(AppConfig.currentUserId(), mediaId, request));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> remove(@PathVariable Long mediaId) {
        libraryService.remove(AppConfig.currentUserId(), mediaId);
        return ResponseEntity.noContent().build();
    }
}
