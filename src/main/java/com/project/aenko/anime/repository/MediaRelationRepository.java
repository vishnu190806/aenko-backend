package com.project.aenko.anime.repository;

import com.project.aenko.anime.entity.MediaRelation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRelationRepository extends JpaRepository<MediaRelation, Long> {
    List<MediaRelation> findBySourceMediaId(Long sourceMediaId);
}
