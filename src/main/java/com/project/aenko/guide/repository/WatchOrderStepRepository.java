package com.project.aenko.guide.repository;

import com.project.aenko.guide.entity.WatchOrderStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchOrderStepRepository extends JpaRepository<WatchOrderStep, Long> {
    List<WatchOrderStep> findByWatchOrderIdOrderBySequenceNumberAsc(Long watchOrderId);
}
