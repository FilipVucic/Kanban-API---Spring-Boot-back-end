package com.hivetech.kanban.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivetech.kanban.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        outputStream = new ByteArrayOutputStream();
        
        given(response.getOutputStream()).willReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener listener) {
            }

            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }
        });
    }

    @Test
    @DisplayName("should return 401 status for unauthorized request")
    void shouldReturn401Status() throws IOException {
        // given
        given(authException.getMessage()).willReturn("Full authentication is required");
        given(request.getRequestURI()).willReturn("/api/tasks");

        // when
        entryPoint.commence(request, response, authException);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("should write error response with correct fields")
    void shouldWriteErrorResponse() throws IOException {
        // given
        given(authException.getMessage()).willReturn("Full authentication is required");
        given(request.getRequestURI()).willReturn("/api/tasks");

        // when
        entryPoint.commence(request, response, authException);

        // then
        String responseBody = outputStream.toString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getError()).isEqualTo("Unauthorized");
        assertThat(errorResponse.getMessage()).contains("Authentication required");
        assertThat(errorResponse.getPath()).isEqualTo("/api/tasks");
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("should handle different request paths")
    void shouldHandleDifferentPaths() throws IOException {
        // given
        given(authException.getMessage()).willReturn("Token expired");
        given(request.getRequestURI()).willReturn("/api/tasks/1");

        // when
        entryPoint.commence(request, response, authException);

        // then
        String responseBody = outputStream.toString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.getPath()).isEqualTo("/api/tasks/1");
    }
}

