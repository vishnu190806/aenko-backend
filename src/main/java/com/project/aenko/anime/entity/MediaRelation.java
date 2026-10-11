package com.project.aenko.anime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_relation")
public class MediaRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceMediaId;

    @Column(nullable = false)
    private Long targetMediaId;

    @Column(nullable = false, length = 30)
    private String relationType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceMediaId() {
        return sourceMediaId;
    }

    public void setSourceMediaId(Long sourceMediaId) {
        this.sourceMediaId = sourceMediaId;
    }

    public Long getTargetMediaId() {
        return targetMediaId;
    }

    public void setTargetMediaId(Long targetMediaId) {
        this.targetMediaId = targetMediaId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
