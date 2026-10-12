package com.college.budget.api;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return response(e.status(), e.getMessage(), Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x -> fields.putIfAbsent(x.getField(), x.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Please check the entered values.", fields);
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<?> invalid(RuntimeException e) { return response(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception e) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request. Check amounts, dates, required fields and transaction type.", Map.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate(DataIntegrityViolationException e) {
        return response(HttpStatus.CONFLICT, "This value already exists or is used by another record. Refresh and try again.", Map.of());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<?> notFound(NoResourceFoundException e) {
        return response(HttpStatus.NOT_FOUND, "The requested resource was not found.", Map.of());
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> wrongMethod(HttpRequestMethodNotSupportedException e) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for the route.", Map.of());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        LOG.error("Unexpected application error: {}", e.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed. Please try again later.", Map.of());
    }
    private ResponseEntity<?> response(HttpStatus status, String message, Map<String,String> errors) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message, "errors", errors));
    }
}
