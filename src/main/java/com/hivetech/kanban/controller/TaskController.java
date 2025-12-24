package com.hivetech.kanban.controller;

import com.hivetech.kanban.dto.*;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management API")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieve a paginated list of tasks with optional status filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedModel<EntityModel<TaskResponse>>> getAllTasks(
            @Parameter(description = "Filter by status") @RequestParam(required = false) Status status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<TaskResponse> tasks = taskService.getAllTasks(status, pageable);
        
        PagedModel<EntityModel<TaskResponse>> pagedModel = PagedModel.of(
                tasks.getContent().stream()
                        .map(task -> EntityModel.of(task, createTaskLink(task.getId())))
                        .toList(),
                new PagedModel.PageMetadata(
                        tasks.getSize(),
                        tasks.getNumber(),
                        tasks.getTotalElements(),
                        tasks.getTotalPages()
                )
        );
        
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieve a specific task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved task"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EntityModel<TaskResponse>> getTaskById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        
        TaskResponse task = taskService.getTaskById(id);
        EntityModel<TaskResponse> model = EntityModel.of(task, createTaskLink(id));
        
        return ResponseEntity.ok(model);
    }

    @PostMapping
    @Operation(summary = "Create a new task", description = "Create a new task with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EntityModel<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request) {
        
        TaskResponse task = taskService.createTask(request);
        EntityModel<TaskResponse> model = EntityModel.of(task, createTaskLink(task.getId()));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Fully update an existing task (requires version for optimistic locking)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - version mismatch", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EntityModel<TaskResponse>> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        
        TaskResponse task = taskService.updateTask(id, request);
        EntityModel<TaskResponse> model = EntityModel.of(task, createTaskLink(id));
        
        return ResponseEntity.ok(model);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Partially update a task", description = "Partially update an existing task using JSON Merge Patch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task patched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EntityModel<TaskResponse>> patchTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody TaskPatchRequest request) {
        
        TaskResponse task = taskService.patchTask(id, request);
        EntityModel<TaskResponse> model = EntityModel.of(task, createTaskLink(id));
        
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "Delete an existing task by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private org.springframework.hateoas.Link createTaskLink(Long taskId) {
        return linkTo(methodOn(TaskController.class).getTaskById(taskId)).withSelfRel();
    }
}

