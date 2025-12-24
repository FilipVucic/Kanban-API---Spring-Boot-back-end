package com.hivetech.kanban.mapper;

import com.hivetech.kanban.dto.TaskPatchRequest;
import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.dto.TaskResponse;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        // Instantiate the MapStruct generated implementation directly
        // Since it's a simple mapper with no dependencies, we can create it directly
        taskMapper = new TaskMapperImpl();
    }

    @Test
    @DisplayName("should map TaskRequest to Task entity")
    void shouldMapTaskRequestToEntity() {
        // given
        TaskRequest request = TaskRequest.builder()
                .title("Test Task")
                .description("Test Description")
                .status("TO_DO")
                .priority("HIGH")
                .build();

        // when
        Task task = taskMapper.toEntity(request);

        // then
        assertThat(task.getTitle()).isEqualTo("Test Task");
        assertThat(task.getDescription()).isEqualTo("Test Description");
        assertThat(task.getStatus()).isEqualTo(Status.TO_DO);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getId()).isNull();
    }

    @Test
    @DisplayName("should map Task entity to TaskResponse")
    void shouldMapTaskToResponse() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Task task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .version(2L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        TaskResponse response = taskMapper.toResponse(task);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Task");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(response.getVersion()).isEqualTo(2L);
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should map task with default status and priority values")
    void shouldMapTaskWithDefaultValues() {
        // given
        TaskRequest request = TaskRequest.builder()
                .title("Test Task")
                .status("TO_DO")
                .priority("MEDIUM")
                .build();

        // when
        Task task = taskMapper.toEntity(request);

        // then
        assertThat(task.getStatus()).isEqualTo(Status.TO_DO);
        assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("should patch entity with only provided fields")
    void shouldPatchEntityWithOnlyProvidedFields() {
        // given
        Task task = Task.builder()
                .id(1L)
                .title("Original Title")
                .description("Original Description")
                .status(Status.TO_DO)
                .priority(Priority.LOW)
                .build();

        TaskPatchRequest patchRequest = TaskPatchRequest.builder()
                .title("Updated Title")
                .build();

        // when
        taskMapper.patchEntity(task, patchRequest);

        // then
        assertThat(task.getTitle()).isEqualTo("Updated Title");
        assertThat(task.getDescription()).isEqualTo("Original Description");
        assertThat(task.getStatus()).isEqualTo(Status.TO_DO);
        assertThat(task.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("should handle case-insensitive enum conversion")
    void shouldHandleCaseInsensitiveEnumConversion() {
        // given
        TaskRequest request = TaskRequest.builder()
                .title("Test Task")
                .status("in_progress")
                .priority("high")
                .build();

        // when
        Task task = taskMapper.toEntity(request);

        // then
        assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
    }
}

