package server.demo.controller.advice;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.controller.InternalTaskController;
import server.demo.dto.ApiResponse;
import server.demo.exception.InternalTaskConflictException;
import server.demo.exception.InternalTaskNotFoundException;

@RestControllerAdvice(assignableTypes = InternalTaskController.class)
public class InternalTaskExceptionHandler {
    @ExceptionHandler(InternalTaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> notFound(InternalTaskNotFoundException exception) {
        return ResponseEntity.status(404).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler({InternalTaskConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Object>> conflict(RuntimeException exception) {
        String message = exception instanceof InternalTaskConflictException
                ? exception.getMessage() : "任务已被其他人更新，请刷新后重试";
        return ResponseEntity.status(409).body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
    }
}
