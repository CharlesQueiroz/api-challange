package com.example.ecommerce.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String entityName, String fieldName, String fieldValue) {
        super("%s with %s '%s' already exists".formatted(entityName, fieldName, fieldValue));
    }
}
