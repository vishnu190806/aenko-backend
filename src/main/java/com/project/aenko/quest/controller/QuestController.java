package com.project.aenko.quest.controller;

import com.project.aenko.common.config.AppConfig;
import com.project.aenko.quest.dto.QuestDto;
import com.project.aenko.quest.service.QuestService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping
    public ResponseEntity<List<QuestDto>> list() {
        return ResponseEntity.ok(questService.listAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<QuestDto>> my() {
        return ResponseEntity.ok(questService.myQuests(AppConfig.currentUserId()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<QuestDto> start(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questService.start(AppConfig.currentUserId(), id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<QuestDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(questService.complete(AppConfig.currentUserId(), id));
    }
}
