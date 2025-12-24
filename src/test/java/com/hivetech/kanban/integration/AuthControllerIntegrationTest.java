package com.hivetech.kanban.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivetech.kanban.dto.AuthRequest;
import com.hivetech.kanban.repository.UserRepository;
import com.hivetech.kanban.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration")
class AuthControllerIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kanban_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword123";

    @BeforeEach
    void setUp() {
        // Create a test user before each test
        userService.register(TEST_USERNAME, TEST_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        // Clean up test user after each test
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /auth/login - should return JWT token for valid credentials")
    void shouldReturnTokenForValidCredentials() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    @Test
    @DisplayName("POST /auth/login - should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - should return 400 for missing username")
    void shouldReturn400ForMissingUsername() throws Exception {
        String invalidJson = "{\"password\": \"" + TEST_PASSWORD + "\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - should register new user and return JWT token")
    void shouldRegisterNewUserAndReturnToken() throws Exception {
        String newUsername = "newuser";
        String newPassword = "newpassword123";
        
        AuthRequest request = AuthRequest.builder()
                .username(newUsername)
                .password(newPassword)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(newUsername));
    }

    @Test
    @DisplayName("POST /auth/register - should return 409 for duplicate username")
    void shouldReturn409ForDuplicateUsername() throws Exception {
        // TEST_USERNAME is already registered in setUp()
        AuthRequest request = AuthRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/register - should return 400 for missing password")
    void shouldReturn400ForMissingPassword() throws Exception {
        String invalidJson = "{\"username\": \"someuser\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - should return 400 for blank username")
    void shouldReturn400ForBlankUsername() throws Exception {
        String invalidJson = "{\"username\": \"\", \"password\": \"testpass\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}

