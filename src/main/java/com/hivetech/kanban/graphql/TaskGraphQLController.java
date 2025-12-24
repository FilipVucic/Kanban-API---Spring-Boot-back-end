package com.hivetech.kanban.graphql;

import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.dto.TaskResponse;
import com.hivetech.kanban.dto.TaskUpdateRequest;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL Controller for Task operations.
 * 
 * This is similar to TaskController but for GraphQL.
 * - @QueryMapping = handles "type Query" from schema (read operations)
 * - @MutationMapping = handles "type Mutation" from schema (write operations)
 * - @Argument = extracts arguments from the GraphQL query
 */
@Controller
@RequiredArgsConstructor
public class TaskGraphQLController {

    private final TaskService taskService;

    // ==================== QUERIES ====================

    /**
     * Get all tasks, optionally filtered by status.
     * 
     * GraphQL query example:
     *   query {
     *     tasks(status: TO_DO) {
     *       id
     *       title
     *       status
     *     }
     *   }
     */
    @QueryMapping
    public List<TaskGraphQL> tasks(@Argument Status status) {
        // Use unpaged to get all tasks (GraphQL clients handle their own pagination)
        return taskService.getAllTasks(status, Pageable.unpaged()).getContent()
                .stream()
                .map(this::toGraphQL)
                .toList();
    }

    /**
     * Get a single task by ID.
     * 
     * GraphQL query example:
     *   query {
     *     task(id: 1) {
     *       id
     *       title
     *       description
     *       status
     *       priority
     *     }
     *   }
     */
    @QueryMapping
    public TaskGraphQL task(@Argument Long id) {
        return toGraphQL(taskService.getTaskById(id));
    }

    // ==================== MUTATIONS ====================

    /**
     * Create a new task.
     * 
     * GraphQL mutation example:
     *   mutation {
     *     createTask(input: {
     *       title: "New Task"
     *       description: "Task description"
     *       status: "TO_DO"
     *       priority: "HIGH"
     *     }) {
     *       id
     *       title
     *       status
     *     }
     *   }
     */
    @MutationMapping
    public TaskGraphQL createTask(@Argument CreateTaskInput input) {
        TaskRequest request = new TaskRequest();
        request.setTitle(input.title());
        request.setDescription(input.description());
        request.setStatus(input.status());
        request.setPriority(input.priority());
        
        return toGraphQL(taskService.createTask(request));
    }

    /**
     * Update an existing task.
     * 
     * GraphQL mutation example:
     *   mutation {
     *     updateTask(id: 1, input: {
     *       title: "Updated Title"
     *       description: "Updated description"
     *       status: "IN_PROGRESS"
     *       priority: "MEDIUM"
     *       version: 0
     *     }) {
     *       id
     *       title
     *       version
     *     }
     *   }
     */
    @MutationMapping
    public TaskGraphQL updateTask(@Argument Long id, @Argument UpdateTaskInput input) {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle(input.title());
        request.setDescription(input.description());
        request.setStatus(input.status());
        request.setPriority(input.priority());
        request.setVersion(input.version());
        
        return toGraphQL(taskService.updateTask(id, request));
    }

    /**
     * Delete a task.
     * 
     * GraphQL mutation example:
     *   mutation {
     *     deleteTask(id: 1)
     *   }
     */
    @MutationMapping
    public boolean deleteTask(@Argument Long id) {
        taskService.deleteTask(id);
        return true;
    }

    // ==================== HELPER METHOD ====================
    
    private TaskGraphQL toGraphQL(TaskResponse response) {
        return new TaskGraphQL(
                response.getId(),
                response.getTitle(),
                response.getDescription(),
                response.getStatus(),
                response.getPriority(),
                response.getVersion(),
                response.getCreatedAt(),
                response.getUpdatedAt()
        );
    }

    // ==================== GRAPHQL TYPES ====================
    // Simple record for GraphQL serialization (no HATEOAS complexity)
    
    record TaskGraphQL(
        Long id,
        String title,
        String description,
        Status status,
        Priority priority,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    // ==================== INPUT RECORDS ====================
    // These map to the "input" types in the GraphQL schema
    
    record CreateTaskInput(
        String title,
        String description,
        String status,
        String priority
    ) {}

    record UpdateTaskInput(
        String title,
        String description,
        String status,
        String priority,
        Long version
    ) {}
}

