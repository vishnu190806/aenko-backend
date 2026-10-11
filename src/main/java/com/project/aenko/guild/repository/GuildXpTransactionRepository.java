package com.project.aenko.guild.repository;

import com.project.aenko.guild.entity.GuildXpTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GuildXpTransactionRepository extends JpaRepository<GuildXpTransaction, Long> {

    @Query("select coalesce(sum(t.amount), 0) from GuildXpTransaction t where t.guildId = :guildId")
    long totalXpByGuild(Long guildId);

    List<GuildXpTransaction> findByGuildId(Long guildId);

    boolean existsByGuildIdAndSourceTypeAndSourceId(Long guildId, String sourceType, String sourceId);

    @Query("select t.userId as userId, sum(t.amount) as xp from GuildXpTransaction t "
            + "where t.guildId = :guildId group by t.userId order by sum(t.amount) desc")
    List<GuildLeaderboardRow> leaderboardByGuild(Long guildId);

    interface GuildLeaderboardRow {
        Long getUserId();

        Long getXp();
    }
}
