package com.project.aenko.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.aenko.auth.entity.User;
import com.project.aenko.auth.repository.UserRepository;
import com.project.aenko.progression.entity.XpTransaction;
import com.project.aenko.progression.repository.XpTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @TestConfiguration
    static class CacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Test
    void userLookupsAndExistenceChecks() {
        User user = new User();
        user.setUsername("goku");
        user.setEmail("goku@aenko.dev");
        user.setPassword("hashed");
        user.setRole(User.Role.USER);
        userRepository.save(user);

        assertThat(userRepository.findByEmail("goku@aenko.dev")).isPresent();
        assertThat(userRepository.existsByEmail("goku@aenko.dev")).isTrue();
        assertThat(userRepository.existsByUsername("goku")).isTrue();
        assertThat(userRepository.existsByEmail("missing@aenko.dev")).isFalse();
    }

    @Test
    void xpTotalsAreSummedPerUser() {
        XpTransaction first = new XpTransaction();
        first.setUserId(1L);
        first.setSourceType("EPISODE");
        first.setSourceId("1:1");
        first.setAmount(20);
        xpTransactionRepository.save(first);

        XpTransaction second = new XpTransaction();
        second.setUserId(1L);
        second.setSourceType("EPISODE");
        second.setSourceId("1:2");
        second.setAmount(20);
        xpTransactionRepository.save(second);

        assertThat(xpTransactionRepository.totalXpByUser(1L)).isEqualTo(40L);
        assertThat(xpTransactionRepository.totalXpByUser(2L)).isEqualTo(0L);
    }
}
