package server.demo.controller.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.controller.RegistrationAdminController;
import server.demo.dto.ApiResponse;
import server.demo.exception.RegistrationReviewConflictException;

@RestControllerAdvice(assignableTypes = RegistrationAdminController.class)
public class RegistrationAdminExceptionHandler {
    @ExceptionHandler(RegistrationReviewConflictException.class)
    public ResponseEntity<ApiResponse<Object>> conflict(RegistrationReviewConflictException exception) {
        return ResponseEntity.status(409).body(ApiResponse.error(exception.getMessage()));
    }
}
