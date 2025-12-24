package com.hivetech.kanban.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivetech.kanban.dto.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskEvent {

    private TaskEventType type;
    private TaskResponse task;
    private Long taskId;
}

