package com.hivetech.kanban.integration;

import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.security.JwtTokenProvider;
import com.hivetech.kanban.service.UserService;
import com.hivetech.kanban.websocket.TaskEvent;
import com.hivetech.kanban.websocket.TaskEventType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kanban_test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    private WebSocketStompClient stompClient;
    private String authToken;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword123";

    @BeforeAll
    void setUpUser() {
        userService.register(TEST_USERNAME, TEST_PASSWORD);
        authToken = jwtTokenProvider.generateToken(TEST_USERNAME);
    }

    @BeforeEach
    void setUp() {
        // Initialize WebSocket client for each test (using pure WebSocket, not SockJS)
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        
        // Configure ObjectMapper to handle HATEOAS responses and Java Time
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("Should broadcast task created event via WebSocket")
    void shouldBroadcastTaskCreatedEvent() throws Exception {
        CompletableFuture<TaskEvent> eventFuture = new CompletableFuture<>();

        String wsUrl = "ws://localhost:" + port + "/ws";

        StompSession session = stompClient
                .connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        session.subscribe("/topic/tasks", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TaskEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                TaskEvent event = (TaskEvent) payload;
                if (event != null && event.getTask() != null) {
                    eventFuture.complete(event);
                }
            }
        });

        // Wait a bit for subscription to be established
        Thread.sleep(1000);

        // Create a task via REST API (using TestRestTemplate to hit the real server)
        TaskRequest request = TaskRequest.builder()
                .title("WebSocket Test Task")
                .description("Testing WebSocket notifications")
                .status("TO_DO")
                .priority("HIGH")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);

        HttpEntity<TaskRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/tasks", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Wait for WebSocket event
        TaskEvent event = eventFuture.get(10, TimeUnit.SECONDS);

        assertThat(event).isNotNull();
        assertThat(event.getType()).isEqualTo(TaskEventType.CREATED);
        assertThat(event.getTask()).isNotNull();
        assertThat(event.getTask().getTitle()).isEqualTo("WebSocket Test Task");

        session.disconnect();
    }
}

