package com.project.aenko.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.aenko.common.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-key-for-tests-only-please-use-32-chars-min!!", 86400000L);

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtService.generateToken(42L, "goku", "USER");
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("username")).isEqualTo("goku");
        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwtService.userIdFrom(token)).isEqualTo(42L);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(1L, "goku", "USER");
        assertThatThrownBy(() -> jwtService.parseToken(token + "tampered"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLived =
                new JwtService("test-secret-key-for-tests-only-please-use-32-chars-min!!", 1L);
        String token = shortLived.generateToken(1L, "goku", "USER");
        Thread.sleep(10L);
        assertThatThrownBy(() -> shortLived.parseToken(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
