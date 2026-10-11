package com.project.aenko.ai.repository;

import com.project.aenko.ai.entity.AiInsight;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {
    List<AiInsight> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
