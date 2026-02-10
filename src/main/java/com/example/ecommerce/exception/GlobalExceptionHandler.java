package com.example.ecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String NOT_FOUND_MESSAGE = "Not Found";
    public static final String CONFLICT_MESSAGE = "Conflict";
    public static final String BAD_REQUEST_MESSAGE = "Bad Request";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";
    public static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    public static final String MALFORMED_REQUEST_BODY_MESSAGE = "Malformed request body";

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    public ProblemDetail handleNotFound(Exception ex, HttpServletRequest request) {
        return notFound(ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var problem = badRequest(VALIDATION_FAILED_MESSAGE, request);

        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value")
                ))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class,
            DuplicateResourceException.class
    })
    public ProblemDetail handleConflict(Exception ex, HttpServletRequest request) {
        var detail = switch (ex) {
            case ObjectOptimisticLockingFailureException ignored -> "Resource was modified by another request";
            case DataIntegrityViolationException dataIntegrityEx -> DataIntegrityViolationClassifier.resolveDetail(dataIntegrityEx);
            case DuplicateResourceException duplicateResourceEx -> duplicateResourceEx.getMessage();
            default -> CONFLICT_MESSAGE;
        };
        return conflict(detail, request);
    }

    @ExceptionHandler({
            InsufficientStockException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        return badRequest(resolveBadRequestDetail(ex), request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return problem(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE, "An unexpected error occurred", request);
    }

    private ProblemDetail badRequest(String detail, HttpServletRequest request) {
        return problem(BAD_REQUEST, BAD_REQUEST_MESSAGE, detail, request);
    }

    private ProblemDetail conflict(String detail, HttpServletRequest request) {
        return problem(CONFLICT, CONFLICT_MESSAGE, detail, request);
    }

    private ProblemDetail notFound(String detail, HttpServletRequest request) {
        return problem(NOT_FOUND, NOT_FOUND_MESSAGE, detail, request);
    }

    private String resolveBadRequestDetail(Exception ex) {
        return switch (ex) {
            case HttpMessageNotReadableException ignored -> MALFORMED_REQUEST_BODY_MESSAGE;
            case MethodArgumentTypeMismatchException mismatch -> "Invalid value for parameter: " + mismatch.getName();
            default -> ex.getMessage();
        };
    }

    private ProblemDetail problem(HttpStatusCode status, String title, String detail, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
