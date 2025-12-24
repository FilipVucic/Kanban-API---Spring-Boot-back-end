package com.hivetech.kanban.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.dto.TaskUpdateRequest;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.repository.TaskRepository;
import com.hivetech.kanban.repository.UserRepository;
import com.hivetech.kanban.security.JwtTokenProvider;
import com.hivetech.kanban.service.UserService;
import org.junit.jupiter.api.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskControllerIntegrationTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private String authToken;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword123";

    @BeforeEach
    void setUp() {
        userService.register(TEST_USERNAME, TEST_PASSWORD);
        authToken = jwtTokenProvider.generateToken(TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        // Delete tasks first, then users to avoid any potential constraint issues
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/tasks - should create a new task")
    void shouldCreateTask() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("Integration Test Task")
                .description("Testing task creation")
                .status("TO_DO")
                .priority("HIGH")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("Testing task creation"))
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/tasks - should return paginated tasks")
    void shouldReturnPaginatedTasks() throws Exception {
        // Create test tasks
        createTestTask("Task 1", Status.TO_DO);
        createTestTask("Task 2", Status.IN_PROGRESS);
        createTestTask("Task 3", Status.DONE);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/tasks - should filter by status")
    void shouldFilterByStatus() throws Exception {
        createTestTask("Task 1", Status.TO_DO);
        createTestTask("Task 2", Status.TO_DO);
        createTestTask("Task 3", Status.DONE);

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .param("status", "TO_DO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/tasks/{id} - should return task by id")
    void shouldReturnTaskById() throws Exception {
        Task task = createTestTask("Find Me Task", Status.TO_DO);

        mockMvc.perform(get("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Find Me Task"))
                .andExpect(jsonPath("$.id").value(task.getId()));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/tasks/{id} - should return 404 for non-existent task")
    void shouldReturn404ForNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 99999L)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/tasks/{id} - should update task")
    void shouldUpdateTask() throws Exception {
        Task task = createTestTask("Original Title", Status.TO_DO);
        
        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .version(task.getVersion())
                .build();

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getVersion()).isEqualTo(task.getVersion() + 1);
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/tasks/{id} - should return 409 on version conflict")
    void shouldReturn409OnVersionConflict() throws Exception {
        Task task = createTestTask("Conflict Task", Status.TO_DO);

        TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                .title("Updated Title")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .version(999L) // Wrong version
                .build();

        mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    @DisplayName("PATCH /api/tasks/{id} - should partially update task")
    void shouldPatchTask() throws Exception {
        Task task = createTestTask("Patch Me", Status.TO_DO);

        String patchJson = "{\"status\": \"DONE\"}";

        mockMvc.perform(patch("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patch Me"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/tasks/{id} - should delete task")
    void shouldDeleteTask() throws Exception {
        Task task = createTestTask("Delete Me", Status.TO_DO);

        mockMvc.perform(delete("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("Should return 401 without authentication")
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/tasks - should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        String invalidJson = "{\"title\": \"\"}"; // Empty title

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @Order(12)
    @DisplayName("POST /api/tasks - should return 400 for malformed JSON")
    void shouldReturn400ForMalformedJson() throws Exception {
        String malformedJson = "{\"title\": \"test\""; // Missing closing brace

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    private Task createTestTask(String title, Status status) {
        Task task = Task.builder()
                .title(title)
                .description("Test description")
                .status(status)
                .priority(Priority.MEDIUM)
                .build();
        Task savedTask = taskRepository.saveAndFlush(task); // Flush to ensure version is set
        return savedTask;
    }
}

