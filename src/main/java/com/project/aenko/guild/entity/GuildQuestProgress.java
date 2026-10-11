package com.project.aenko.guild.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "guild_quest_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"guildQuestId", "userId"}))
public class GuildQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long guildQuestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int contribution = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGuildQuestId() {
        return guildQuestId;
    }

    public void setGuildQuestId(Long guildQuestId) {
        this.guildQuestId = guildQuestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }
}
