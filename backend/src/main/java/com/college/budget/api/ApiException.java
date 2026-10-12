package com.college.budget.api;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus status() { return status; }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND, "Record not found."); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
}
