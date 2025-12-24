package com.hivetech.kanban.websocket;

import com.hivetech.kanban.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskWebSocketService {

    private static final String TASK_TOPIC = "/topic/tasks";
    
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyTaskCreated(TaskResponse task) {
        log.debug("Broadcasting task created event: {}", task.getId());
        
        TaskEvent event = TaskEvent.builder()
                .type(TaskEventType.CREATED)
                .task(task)
                .build();
        
        messagingTemplate.convertAndSend(TASK_TOPIC, event);
    }

    public void notifyTaskUpdated(TaskResponse task) {
        log.debug("Broadcasting task updated event: {}", task.getId());
        
        TaskEvent event = TaskEvent.builder()
                .type(TaskEventType.UPDATED)
                .task(task)
                .build();
        
        messagingTemplate.convertAndSend(TASK_TOPIC, event);
    }

    public void notifyTaskDeleted(Long taskId) {
        log.debug("Broadcasting task deleted event: {}", taskId);
        
        TaskEvent event = TaskEvent.builder()
                .type(TaskEventType.DELETED)
                .taskId(taskId)
                .build();
        
        messagingTemplate.convertAndSend(TASK_TOPIC, event);
    }
}

