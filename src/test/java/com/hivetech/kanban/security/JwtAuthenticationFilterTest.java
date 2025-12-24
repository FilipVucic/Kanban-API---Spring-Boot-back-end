package com.hivetech.kanban.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("should set authentication when valid token is provided")
    void shouldSetAuthenticationWhenValidTokenProvided() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;
        
        given(request.getHeader("Authorization")).willReturn(bearerToken);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(token)).willReturn("testuser");
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(userDetails);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUsernameFromToken(token);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(filterChain).doFilter(request, response);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("should not set authentication when no token is provided")
    void shouldNotSetAuthenticationWhenNoTokenProvided() throws ServletException, IOException {
        // given
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should not set authentication when token is invalid")
    void shouldNotSetAuthenticationWhenTokenInvalid() throws ServletException, IOException {
        // given
        String token = "invalid.jwt.token";
        String bearerToken = "Bearer " + token;
        
        given(request.getHeader("Authorization")).willReturn(bearerToken);
        given(jwtTokenProvider.validateToken(token)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUsernameFromToken(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("should handle exception gracefully and continue filter chain")
    void shouldHandleExceptionGracefully() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;
        
        given(request.getHeader("Authorization")).willReturn(bearerToken);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(token)).willThrow(new RuntimeException("Token error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // Filter should continue even if exception occurs
    }

    @Test
    @DisplayName("should extract token from Bearer header correctly")
    void shouldExtractTokenFromBearerHeader() throws ServletException, IOException {
        // given
        String token = "extracted.token.here";
        String bearerToken = "Bearer " + token;
        
        given(request.getHeader("Authorization")).willReturn(bearerToken);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUsernameFromToken(token)).willReturn("testuser");
        given(userDetailsService.loadUserByUsername("testuser")).willReturn(userDetails);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUsernameFromToken(token);
    }
}

