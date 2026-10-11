package com.project.aenko.guild.repository;

import com.project.aenko.guild.entity.GuildQuest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildQuestRepository extends JpaRepository<GuildQuest, Long> {
    List<GuildQuest> findByGuildId(Long guildId);

    List<GuildQuest> findByGuildIdAndStatus(Long guildId, String status);
}
