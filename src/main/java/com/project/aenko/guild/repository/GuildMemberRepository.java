package com.project.aenko.guild.repository;

import com.project.aenko.guild.entity.GuildMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildMemberRepository extends JpaRepository<GuildMember, Long> {
    Optional<GuildMember> findByGuildIdAndUserId(Long guildId, Long userId);

    List<GuildMember> findByGuildId(Long guildId);

    List<GuildMember> findByUserId(Long userId);

    long countByGuildId(Long guildId);

    long countByUserId(Long userId);
}
