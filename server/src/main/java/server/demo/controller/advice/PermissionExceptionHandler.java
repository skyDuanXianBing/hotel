package server.demo.controller.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.dto.ApiResponse;
import server.demo.exception.PermissionDeniedException;

@RestControllerAdvice
public class PermissionExceptionHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handlePermissionDenied(PermissionDeniedException e) {
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }
}

