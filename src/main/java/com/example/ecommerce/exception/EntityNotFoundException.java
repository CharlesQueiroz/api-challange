package com.example.ecommerce.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;

    public EntityNotFoundException(String entityName, UUID code) {
        super("%s with code %s not found".formatted(entityName, code));
        this.entityName = entityName;
    }

    public EntityNotFoundException(String entityName, Long id) {
        super("%s with id %d not found".formatted(entityName, id));
        this.entityName = entityName;
    }
}
