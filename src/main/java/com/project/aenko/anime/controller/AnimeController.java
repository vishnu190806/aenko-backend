package com.project.aenko.anime.controller;

import com.project.aenko.anime.dto.AnimeDto;
import com.project.aenko.anime.dto.AnimeSearchResponse;
import com.project.aenko.anime.dto.RelatedAnimeDto;
import com.project.aenko.anime.service.AnimeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @GetMapping("/search")
    public ResponseEntity<AnimeSearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(animeService.search(query, page, size));
    }

    @GetMapping("/ranking")
    public ResponseEntity<AnimeSearchResponse> ranking(
            @RequestParam(value = "type", defaultValue = "all") String rankingType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(animeService.ranking(rankingType, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnimeDto> detail(@PathVariable("id") String id) {
        return ResponseEntity.ok(animeService.detail(id));
    }

    @GetMapping("/{id}/relations")
    public ResponseEntity<List<RelatedAnimeDto>> relations(@PathVariable("id") String id) {
        return ResponseEntity.ok(animeService.related(id));
    }
}
