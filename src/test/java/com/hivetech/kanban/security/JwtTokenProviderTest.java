package com.hivetech.kanban.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String JWT_SECRET = "test-secret-key-256-bits-long-enough-for-hmac-sha256-algorithm-test";
    private static final long JWT_EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // JwtTokenProvider expects the raw secret string and handles encoding internally
        jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, JWT_EXPIRATION_MS);
    }

    @Test
    @DisplayName("should generate token from username")
    void shouldGenerateTokenFromUsername() {
        // given
        String username = "testuser";

        // when
        String token = jwtTokenProvider.generateToken(username);

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("should generate token from authentication")
    void shouldGenerateTokenFromAuthentication() {
        // given
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // when
        String token = jwtTokenProvider.generateToken(authentication);

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // given
        String username = "testuser";
        String token = jwtTokenProvider.generateToken(username);

        // when
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("should validate valid token")
    void shouldValidateValidToken() {
        // given
        String token = jwtTokenProvider.generateToken("testuser");

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("should reject invalid token")
    void shouldRejectInvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("should reject malformed token")
    void shouldRejectMalformedToken() {
        // given
        String malformedToken = "not.a.valid.jwt.token.format";

        // when
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }
}

