package com.project.aenko.guide.controller;

import com.project.aenko.guide.dto.FranchiseDto;
import com.project.aenko.guide.dto.WatchOrderDto;
import com.project.aenko.guide.service.GuideService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/franchises")
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    @GetMapping
    public ResponseEntity<List<FranchiseDto>> franchises() {
        return ResponseEntity.ok(guideService.franchises());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FranchiseDto> franchise(@PathVariable Long id) {
        return ResponseEntity.ok(guideService.franchise(id));
    }

    @GetMapping("/{id}/watch-order")
    public ResponseEntity<List<WatchOrderDto>> watchOrder(@PathVariable Long id) {
        return ResponseEntity.ok(guideService.watchOrders(id));
    }
}
