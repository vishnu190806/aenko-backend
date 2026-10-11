package com.project.aenko.guild;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.aenko.guild.entity.Guild;
import com.project.aenko.guild.entity.GuildMember;
import com.project.aenko.guild.repository.GuildMemberRepository;
import com.project.aenko.guild.repository.GuildRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Guild 1-to-many GuildMember JPA relationship:
 * {@code @OneToMany(mappedBy)} on Guild and {@code @ManyToOne} on
 * GuildMember, mapped onto the existing {@code guildId} foreign-key column
 * with no schema change.
 */
@DataJpaTest
@ActiveProfiles("test")
class GuildRelationshipTest {

    @TestConfiguration
    static class CacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private GuildRepository guildRepository;

    @Autowired
    private GuildMemberRepository guildMemberRepository;

    @Autowired
    private org.springframework.boot.jpa.test.autoconfigure.TestEntityManager entityManager;

    private Guild createGuild(String name) {
        Guild guild = new Guild();
        guild.setName(name);
        guild.setOwnerId(99L);
        return guildRepository.save(guild);
    }

    private GuildMember createMember(Long guildId, Long userId, String role) {
        GuildMember member = new GuildMember();
        member.setGuildId(guildId);
        member.setUserId(userId);
        member.setRole(role);
        return guildMemberRepository.save(member);
    }

    @Test
    void guildExposesMembersThroughOneToMany() {
        Guild guild = createGuild("Z Fighters");
        createMember(guild.getId(), 1L, GuildMember.GuildRole.OWNER.name());
        createMember(guild.getId(), 2L, GuildMember.GuildRole.MEMBER.name());

        entityManager.flush();
        entityManager.clear();

        Guild found = guildRepository.findById(guild.getId()).orElseThrow();
        assertThat(found.getMembers()).hasSize(2);
        assertThat(found.getMembers().stream().map(GuildMember::getUserId)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void memberResolvesGuildThroughManyToOne() {
        Guild guild = createGuild("Z Fighters");
        createMember(guild.getId(), 7L, GuildMember.GuildRole.MEMBER.name());

        entityManager.flush();
        entityManager.clear();

        GuildMember found = guildMemberRepository.findByGuildIdAndUserId(guild.getId(), 7L).orElseThrow();
        assertThat(found.getGuild()).isNotNull();
        assertThat(found.getGuild().getId()).isEqualTo(guild.getId());
        assertThat(found.getGuild().getName()).isEqualTo("Z Fighters");
    }

    @Test
    void relationshipMappingLeavesSchemaUnchanged() {
        Guild guild = createGuild("Z Fighters");
        createMember(guild.getId(), 1L, GuildMember.GuildRole.MEMBER.name());

        entityManager.flush();
        entityManager.clear();

        // The association reuses the plain guildId column: writing through the
        // field and reading through the association see the same value.
        GuildMember found = guildMemberRepository.findByGuildId(guild.getId()).get(0);
        assertThat(found.getGuildId()).isEqualTo(guild.getId());
        assertThat(found.getGuild().getId()).isEqualTo(found.getGuildId());
    }
}
