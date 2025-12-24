package com.hivetech.kanban.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {
    
    public ResourceAlreadyExistsException(String resource, String field, String value) {
        super(String.format("%s with %s '%s' already exists", resource, field, value));
    }
}

