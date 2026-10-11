package com.project.aenko.guild.controller;

import com.project.aenko.common.config.AppConfig;
import com.project.aenko.guild.dto.GuildDto;
import com.project.aenko.guild.dto.GuildMemberDto;
import com.project.aenko.guild.dto.GuildQuestDto;
import com.project.aenko.guild.dto.GuildQuestRequest;
import com.project.aenko.guild.dto.GuildRequest;
import com.project.aenko.guild.dto.LeaderboardEntry;
import com.project.aenko.guild.service.GuildService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guilds")
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @PostMapping
    public ResponseEntity<GuildDto> create(@Valid @RequestBody GuildRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guildService.create(AppConfig.currentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<GuildDto>> list() {
        return ResponseEntity.ok(guildService.listPublic());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuildDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(guildService.get(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id) {
        guildService.join(AppConfig.currentUserId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id) {
        guildService.leave(AppConfig.currentUserId(), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GuildMemberDto>> members(@PathVariable Long id) {
        return ResponseEntity.ok(guildService.members(id));
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> leaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(guildService.leaderboard(id));
    }

    @GetMapping("/{id}/quests")
    public ResponseEntity<List<GuildQuestDto>> quests(@PathVariable Long id) {
        return ResponseEntity.ok(guildService.quests(id));
    }

    @PostMapping("/{id}/quests")
    public ResponseEntity<GuildQuestDto> createQuest(@PathVariable Long id,
            @Valid @RequestBody GuildQuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guildService.createQuest(AppConfig.currentUserId(), id, request));
    }
}
