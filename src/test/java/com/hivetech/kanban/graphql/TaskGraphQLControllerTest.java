package com.hivetech.kanban.graphql;

import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.dto.TaskResponse;
import com.hivetech.kanban.dto.TaskUpdateRequest;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskGraphQLControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskGraphQLController taskGraphQLController;

    private TaskResponse taskResponse;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        taskResponse = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.TO_DO)
                .priority(Priority.MEDIUM)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("should return all tasks without status filter")
    void shouldReturnAllTasksWithoutFilter() {
        // given
        Page<TaskResponse> taskPage = new PageImpl<>(List.of(taskResponse));
        given(taskService.getAllTasks(eq(null), any(Pageable.class))).willReturn(taskPage);

        // when
        var result = taskGraphQLController.tasks(null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).title()).isEqualTo("Test Task");
        assertThat(result.get(0).description()).isEqualTo("Test Description");
        assertThat(result.get(0).status()).isEqualTo(Status.TO_DO);
        assertThat(result.get(0).priority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("should return tasks filtered by status")
    void shouldReturnTasksFilteredByStatus() {
        // given
        Page<TaskResponse> taskPage = new PageImpl<>(List.of(taskResponse));
        given(taskService.getAllTasks(eq(Status.TO_DO), any(Pageable.class))).willReturn(taskPage);

        // when
        var result = taskGraphQLController.tasks(Status.TO_DO);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(Status.TO_DO);
    }

    @Test
    @DisplayName("should return task by id")
    void shouldReturnTaskById() {
        // given
        given(taskService.getTaskById(1L)).willReturn(taskResponse);

        // when
        var result = taskGraphQLController.task(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Test Task");
        assertThat(result.version()).isEqualTo(0L);
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should create task")
    void shouldCreateTask() {
        // given
        TaskGraphQLController.CreateTaskInput input = new TaskGraphQLController.CreateTaskInput(
                "New Task",
                "New Description",
                "TO_DO",
                "HIGH"
        );
        given(taskService.createTask(any(TaskRequest.class))).willReturn(taskResponse);

        // when
        var result = taskGraphQLController.createTask(input);

        // then
        assertThat(result).isNotNull();
        verify(taskService).createTask(any(TaskRequest.class));
    }

    @Test
    @DisplayName("should update task")
    void shouldUpdateTask() {
        // given
        TaskGraphQLController.UpdateTaskInput input = new TaskGraphQLController.UpdateTaskInput(
                "Updated Task",
                "Updated Description",
                "IN_PROGRESS",
                "HIGH",
                0L
        );
        TaskResponse updatedResponse = TaskResponse.builder()
                .id(1L)
                .title("Updated Task")
                .description("Updated Description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .version(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();
        given(taskService.updateTask(eq(1L), any(TaskUpdateRequest.class))).willReturn(updatedResponse);

        // when
        var result = taskGraphQLController.updateTask(1L, input);

        // then
        assertThat(result.title()).isEqualTo("Updated Task");
        assertThat(result.status()).isEqualTo(Status.IN_PROGRESS);
        verify(taskService).updateTask(eq(1L), any(TaskUpdateRequest.class));
    }

    @Test
    @DisplayName("should delete task")
    void shouldDeleteTask() {
        // given
        willDoNothing().given(taskService).deleteTask(1L);

        // when
        boolean result = taskGraphQLController.deleteTask(1L);

        // then
        assertThat(result).isTrue();
        verify(taskService).deleteTask(1L);
    }

    @Test
    @DisplayName("should return empty list when no tasks exist")
    void shouldReturnEmptyListWhenNoTasks() {
        // given
        Page<TaskResponse> emptyPage = new PageImpl<>(List.of());
        given(taskService.getAllTasks(any(), any(Pageable.class))).willReturn(emptyPage);

        // when
        var result = taskGraphQLController.tasks(null);

        // then
        assertThat(result).isEmpty();
    }
}

