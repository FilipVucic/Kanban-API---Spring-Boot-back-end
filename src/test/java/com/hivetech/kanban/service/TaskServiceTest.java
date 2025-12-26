package com.hivetech.kanban.service;

import com.hivetech.kanban.dto.*;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.exception.OptimisticLockException;
import com.hivetech.kanban.exception.ResourceNotFoundException;
import com.hivetech.kanban.mapper.TaskMapper;
import com.hivetech.kanban.repository.TaskRepository;
import com.hivetech.kanban.websocket.TaskWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskWebSocketService webSocketService;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskResponse taskResponse;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.TO_DO)
                .priority(Priority.MEDIUM)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskResponse = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.TO_DO)
                .priority(Priority.MEDIUM)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRequest = TaskRequest.builder()
                .title("Test Task")
                .description("Test Description")
                .status("TO_DO")
                .priority("MEDIUM")
                .build();
    }

    @Nested
    @DisplayName("getAllTasks")
    class GetAllTasksTests {

        @Test
        @DisplayName("should return paginated tasks without status filter")
        void shouldReturnAllTasksWithoutFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);
            
            given(taskRepository.findAll(pageable)).willReturn(taskPage);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            // when
            Page<TaskResponse> result = taskService.getAllTasks(null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Task");
            verify(taskRepository).findAll(pageable);
            verify(taskRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("should return filtered tasks by status")
        void shouldReturnTasksFilteredByStatus() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);
            
            given(taskRepository.findByStatus(Status.TO_DO, pageable)).willReturn(taskPage);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            // when
            Page<TaskResponse> result = taskService.getAllTasks(Status.TO_DO, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository).findByStatus(Status.TO_DO, pageable);
        }
    }

    @Nested
    @DisplayName("getTaskById")
    class GetTaskByIdTests {

        @Test
        @DisplayName("should return task when found")
        void shouldReturnTaskWhenFound() {
            // given
            given(taskRepository.findById(1L)).willReturn(Optional.of(task));
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            // when
            TaskResponse result = taskService.getTaskById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Task");
        }

        @Test
        @DisplayName("should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // given
            given(taskRepository.findById(99L)).willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found");
        }
    }

    @Nested
    @DisplayName("createTask")
    class CreateTaskTests {

        @Test
        @DisplayName("should create task successfully")
        void shouldCreateTaskSuccessfully() {
            // given
            given(taskMapper.toEntity(taskRequest)).willReturn(task);
            given(taskRepository.save(task)).willReturn(task);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            // when
            TaskResponse result = taskService.createTask(taskRequest);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Task");
            verify(webSocketService).notifyTaskCreated(taskResponse);
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {

        @Test
        @DisplayName("should update task successfully")
        void shouldUpdateTaskSuccessfully() {
            // given
            TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .status("IN_PROGRESS")
                    .priority("HIGH")
                    .version(0L)
                    .build();

            TaskResponse updatedResponse = TaskResponse.builder()
                    .id(1L)
                    .title("Updated Task")
                    .description("Updated Description")
                    .status(Status.IN_PROGRESS)
                    .priority(Priority.HIGH)
                    .version(1L)
                    .build();

            given(taskRepository.findById(1L)).willReturn(Optional.of(task));
            given(taskRepository.saveAndFlush(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(taskMapper.toResponse(any(Task.class))).willReturn(updatedResponse);

            // when
            TaskResponse result = taskService.updateTask(1L, updateRequest);

            // then
            assertThat(result.getTitle()).isEqualTo("Updated Task");
            verify(webSocketService).notifyTaskUpdated(updatedResponse);
        }

        @Test
        @DisplayName("should throw exception on version mismatch")
        void shouldThrowExceptionOnVersionMismatch() {
            // given
            TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                    .title("Updated Task")
                    .version(5L) // Wrong version
                    .build();

            given(taskRepository.findById(1L)).willReturn(Optional.of(task));

            // when/then
            assertThatThrownBy(() -> taskService.updateTask(1L, updateRequest))
                    .isInstanceOf(OptimisticLockException.class);
        }

        @Test
        @DisplayName("should throw OptimisticLockException on concurrent modification during save")
        void shouldThrowExceptionOnConcurrentModificationDuringSave() {
            // given
            // This tests the race condition: two threads pass the version check,
            // but one commits first, causing Hibernate to throw ObjectOptimisticLockingFailureException
            // when the second thread tries to save
            TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .status("IN_PROGRESS")
                    .priority("HIGH")
                    .version(0L) // Correct version at read time
                    .build();

            given(taskRepository.findById(1L)).willReturn(Optional.of(task));
            given(taskRepository.saveAndFlush(any(Task.class)))
                    .willThrow(new ObjectOptimisticLockingFailureException(Task.class, 1L));

            // when/then
            assertThatThrownBy(() -> taskService.updateTask(1L, updateRequest))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessageContaining("Task has been modified by another user");
        }
    }

    @Nested
    @DisplayName("patchTask")
    class PatchTaskTests {

        @Test
        @DisplayName("should patch task successfully")
        void shouldPatchTaskSuccessfully() {
            // given
            TaskPatchRequest patchRequest = TaskPatchRequest.builder()
                    .status("DONE")
                    .build();

            TaskResponse patchedResponse = TaskResponse.builder()
                    .id(1L)
                    .title("Test Task")
                    .description("Test Description")
                    .status(Status.DONE)
                    .priority(Priority.MEDIUM)
                    .version(1L)
                    .build();

            given(taskRepository.findById(1L)).willReturn(Optional.of(task));
            willDoNothing().given(taskMapper).patchEntity(any(Task.class), any(TaskPatchRequest.class));
            given(taskRepository.saveAndFlush(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(taskMapper.toResponse(any(Task.class))).willReturn(patchedResponse);

            // when
            TaskResponse result = taskService.patchTask(1L, patchRequest);

            // then
            assertThat(result.getStatus()).isEqualTo(Status.DONE);
            verify(taskMapper).patchEntity(task, patchRequest);
            verify(webSocketService).notifyTaskUpdated(patchedResponse);
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTaskTests {

        @Test
        @DisplayName("should delete task successfully")
        void shouldDeleteTaskSuccessfully() {
            // given
            given(taskRepository.existsById(1L)).willReturn(true);
            willDoNothing().given(taskRepository).deleteById(1L);

            // when
            taskService.deleteTask(1L);

            // then
            verify(taskRepository).deleteById(1L);
            verify(webSocketService).notifyTaskDeleted(1L);
        }

        @Test
        @DisplayName("should throw exception when deleting non-existent task")
        void shouldThrowExceptionWhenDeletingNonExistentTask() {
            // given
            given(taskRepository.existsById(99L)).willReturn(false);

            // when/then
            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

