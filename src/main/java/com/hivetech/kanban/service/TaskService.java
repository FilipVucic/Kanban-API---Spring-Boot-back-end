package com.hivetech.kanban.service;

import com.hivetech.kanban.dto.*;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.exception.ResourceNotFoundException;
import com.hivetech.kanban.exception.OptimisticLockException;
import com.hivetech.kanban.config.CacheConfig;
import com.hivetech.kanban.mapper.TaskMapper;
import com.hivetech.kanban.repository.TaskRepository;
import com.hivetech.kanban.websocket.TaskWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskWebSocketService webSocketService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.TASKS_CACHE, key = "#status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<TaskResponse> getAllTasks(Status status, Pageable pageable) {
        Page<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByStatus(status, pageable);
        } else {
            tasks = taskRepository.findAll(pageable);
        }
        return tasks.map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.TASK_CACHE, key = "#id")
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        
        return taskMapper.toResponse(task);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.TASKS_CACHE, allEntries = true)
    public TaskResponse createTask(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        Task savedTask = taskRepository.save(task);
        
        TaskResponse response = taskMapper.toResponse(savedTask);
        webSocketService.notifyTaskCreated(response);
        
        log.info("Created task with id: {}", savedTask.getId());
        return response;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.TASK_CACHE, CacheConfig.TASKS_CACHE}, allEntries = true)
    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        
        // Check optimistic locking
        if (!task.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockException("Task has been modified by another user. Please refresh and try again.");
        }
        
        try {
            taskMapper.updateEntity(task, request);
            Task savedTask = taskRepository.saveAndFlush(task);
            
            TaskResponse response = taskMapper.toResponse(savedTask);
            webSocketService.notifyTaskUpdated(response);
            
            log.info("Updated task with id: {}", savedTask.getId());
            return response;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new OptimisticLockException("Task has been modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.TASK_CACHE, CacheConfig.TASKS_CACHE}, allEntries = true)
    public TaskResponse patchTask(Long id, TaskPatchRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        
        taskMapper.patchEntity(task, request);
        Task savedTask = taskRepository.saveAndFlush(task);
        
        TaskResponse response = taskMapper.toResponse(savedTask);
        webSocketService.notifyTaskUpdated(response);
        
        log.info("Patched task with id: {}", savedTask.getId());
        return response;
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.TASK_CACHE, CacheConfig.TASKS_CACHE}, allEntries = true)
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", "id", id);
        }
        
        taskRepository.deleteById(id);
        webSocketService.notifyTaskDeleted(id);
        
        log.info("Deleted task with id: {}", id);
    }
}
