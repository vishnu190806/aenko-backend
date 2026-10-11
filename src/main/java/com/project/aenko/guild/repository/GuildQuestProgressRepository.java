package com.project.aenko.guild.repository;

import com.project.aenko.guild.entity.GuildQuestProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildQuestProgressRepository extends JpaRepository<GuildQuestProgress, Long> {
    Optional<GuildQuestProgress> findByGuildQuestIdAndUserId(Long guildQuestId, Long userId);

    List<GuildQuestProgress> findByGuildQuestId(Long guildQuestId);
}
