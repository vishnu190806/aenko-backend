package com.project.aenko.guild.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "guild_member", uniqueConstraints = @UniqueConstraint(columnNames = {"guildId", "userId"}))
public class GuildMember {

    public enum GuildRole {
        OWNER, ADMIN, MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long guildId;

    /**
     * Owning-side mapping of the existing {@code guildId} foreign-key column.
     * Read-only for persistence (the plain {@code guildId} field remains the
     * writer), so the database schema is unchanged.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guildId", insertable = false, updatable = false)
    private Guild guild;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "role_name", nullable = false, length = 20)
    private String role = GuildRole.MEMBER.name();

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long guildId) {
        this.guildId = guildId;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
