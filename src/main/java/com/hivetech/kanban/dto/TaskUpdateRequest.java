package com.hivetech.kanban.dto;

import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Status is required")
    @ValidEnum(enumClass = Status.class, message = "Status must be one of: TO_DO, IN_PROGRESS, DONE")
    private String status;

    @NotNull(message = "Priority is required")
    @ValidEnum(enumClass = Priority.class, message = "Priority must be one of: LOW, MEDIUM, HIGH")
    private String priority;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;
}

