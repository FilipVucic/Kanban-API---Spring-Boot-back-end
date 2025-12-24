package com.hivetech.kanban.mapper;

import com.hivetech.kanban.dto.TaskPatchRequest;
import com.hivetech.kanban.dto.TaskRequest;
import com.hivetech.kanban.dto.TaskResponse;
import com.hivetech.kanban.dto.TaskUpdateRequest;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    Task toEntity(TaskRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    TaskResponse toResponse(Task task);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    Task updateEntity(@MappingTarget Task task, TaskUpdateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    void patchEntity(@MappingTarget Task task, TaskPatchRequest request);

    @Named("stringToStatus")
    default Status stringToStatus(String status) {
        return Status.valueOf(status.toUpperCase());
    }

    @Named("stringToPriority")
    default Priority stringToPriority(String priority) {
        return Priority.valueOf(priority.toUpperCase());
    }
}
