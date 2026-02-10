package com.example.ecommerce.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private static final String REQUEST_URI = "/api/test";

    @Test
    void shouldReturn404_whenEntityNotFound() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var code = UUID.randomUUID();
        var ex = new EntityNotFoundException("Product", code);

        var problem = handler.handleNotFound(ex, request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Not Found");
        assertThat(problem.getDetail()).isEqualTo("Product with code %s not found".formatted(code));
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturn400WithErrors_whenValidationFails() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var fieldError = new FieldError("object", "name", "must not be blank");
        var bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var problem = handler.handleValidation(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);

        var errors = (List<Map<String, String>>) problem.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).containsEntry("field", "name");
        assertThat(errors.getFirst()).containsEntry("message", "must not be blank");
    }

    @Test
    void shouldReturn409_whenOptimisticLockConflict() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = new ObjectOptimisticLockingFailureException("Order", 1L);
        var problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Resource was modified by another request");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn409_whenUniqueConstraintViolated() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var cause = new SQLException("duplicate key value violates unique constraint", "23505");
        var ex = new DataIntegrityViolationException("duplicate key", cause);

        var problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Duplicate value violates uniqueness constraint");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn409_whenForeignKeyViolated() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var cause = new SQLException("insert or update violates foreign key constraint", "23503");
        var ex = new DataIntegrityViolationException("db error", cause);

        var problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Referenced resource not found or invalid");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn409_whenCheckConstraintViolated() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var cause = new SQLException("new row violates check constraint", "23514");
        var ex = new DataIntegrityViolationException("db error", cause);

        var problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Value violates check constraint");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn409_whenDuplicateResourceDetected() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = new DuplicateResourceException("Product", "name", "Wireless Mouse");

        var problem = handler.handleConflict(ex, request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Product with name 'Wireless Mouse' already exists");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn400_whenStockIsInsufficient() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = new InsufficientStockException("Widget X", 5, 10);

        var problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).contains("Insufficient stock for product 'Widget X'");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn400_whenIllegalStateOccurs() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = new IllegalStateException("Invalid state transition");

        var problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Invalid state transition");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn400_whenJsonIsMalformed() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = mock(HttpMessageNotReadableException.class);
        var problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Malformed request body");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn400_whenTypeMismatchOccurs() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("code");

        var problem = handler.handleBadRequest(ex, request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Invalid value for parameter: code");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }

    @Test
    void shouldReturn500_whenUnexpectedErrorOccurs() {
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        var ex = new RuntimeException("unexpected");

        var problem = handler.handleGeneric(ex, request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getInstance()).hasToString(REQUEST_URI);
    }
}
