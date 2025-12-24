package com.hivetech.kanban.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        rateLimitingFilter = new RateLimitingFilter();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        lenient().when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() throws ServletException, IOException {
        // given
        String clientIp = "192.168.1.1";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(clientIp);

        // when - make 10 requests (well within the 100 limit)
        for (int i = 0; i < 10; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // then
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("should block requests exceeding rate limit")
    void shouldBlockRequestsExceedingRateLimit() throws ServletException, IOException {
        // given
        String clientIp = "192.168.1.2";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(clientIp);

        // when - make 101 requests (exceeding the 100 limit)
        for (int i = 0; i < 100; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        // then - first 100 should pass
        verify(filterChain, times(100)).doFilter(request, response);

        // when - 101st request
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then - should be blocked
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        verify(filterChain, times(100)).doFilter(request, response); // Still 100, not 101
        assertThat(stringWriter.toString()).contains("Too Many Requests");
        assertThat(stringWriter.toString()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedForHeader() throws ServletException, IOException {
        // given
        String forwardedIp = "10.0.0.1";
        given(request.getHeader("X-Forwarded-For")).willReturn(forwardedIp);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("should extract IP from X-Real-IP header when X-Forwarded-For is not present")
    void shouldExtractIpFromXRealIpHeader() throws ServletException, IOException {
        // given
        String realIp = "10.0.0.2";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(realIp);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Real-IP");
        verify(request, never()).getRemoteAddr();
    }

    @Test
    @DisplayName("should use RemoteAddr when no proxy headers are present")
    void shouldUseRemoteAddrWhenNoProxyHeaders() throws ServletException, IOException {
        // given
        String remoteAddr = "192.168.1.100";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(remoteAddr);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getRemoteAddr();
    }

    @Test
    @DisplayName("should handle X-Forwarded-For with multiple IPs")
    void shouldHandleXForwardedForWithMultipleIps() throws ServletException, IOException {
        // given
        String forwardedFor = "10.0.0.1, 192.168.1.1, 172.16.0.1";
        given(request.getHeader("X-Forwarded-For")).willReturn(forwardedFor);

        // when
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // Should use the first IP (10.0.0.1)
    }

    @Test
    @DisplayName("should maintain separate rate limits for different IPs")
    void shouldMaintainSeparateRateLimitsForDifferentIps() throws ServletException, IOException {
        // given
        String ip1 = "192.168.1.10";
        String ip2 = "192.168.1.20";
        
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        
        given(request1.getHeader("X-Forwarded-For")).willReturn(null);
        given(request1.getHeader("X-Real-IP")).willReturn(null);
        given(request1.getRemoteAddr()).willReturn(ip1);
        
        given(request2.getHeader("X-Forwarded-For")).willReturn(null);
        given(request2.getHeader("X-Real-IP")).willReturn(null);
        given(request2.getRemoteAddr()).willReturn(ip2);

        // when - each IP makes 100 requests
        for (int i = 0; i < 100; i++) {
            rateLimitingFilter.doFilterInternal(request1, response, filterChain);
            rateLimitingFilter.doFilterInternal(request2, response, filterChain);
        }

        // then - both should succeed (200 requests total, but 100 per IP)
        verify(filterChain, times(200)).doFilter(any(), any());
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("should return JSON error response when rate limit exceeded")
    void shouldReturnJsonErrorResponseWhenRateLimitExceeded() throws ServletException, IOException {
        // given
        String clientIp = "192.168.1.3";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(clientIp);

        // when - exceed limit
        for (int i = 0; i < 100; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("\"error\"");
        assertThat(responseBody).contains("\"message\"");
        assertThat(responseBody).contains("Too Many Requests");
        assertThat(responseBody).contains("100 requests per minute");
    }

    @Test
    @DisplayName("should not call filter chain when rate limit is exceeded")
    void shouldNotCallFilterChainWhenRateLimitExceeded() throws ServletException, IOException {
        // given
        String clientIp = "192.168.1.4";
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(clientIp);

        // when - exceed limit
        for (int i = 0; i < 100; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Reset mock to count only the blocked request
        reset(filterChain);
        
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }
}

