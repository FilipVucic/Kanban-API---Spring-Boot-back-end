package com.hivetech.kanban.dto;

import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse extends RepresentationModel<TaskResponse> {

    private Long id;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
