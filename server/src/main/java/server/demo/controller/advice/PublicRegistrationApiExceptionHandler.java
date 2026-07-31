package server.demo.controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import server.demo.controller.PublicRegistrationBookingController;
import server.demo.controller.PublicRegistrationController;
import server.demo.dto.ApiResponse;

import server.demo.i18n.ApiMessages;
@RestControllerAdvice(assignableTypes = {
        PublicRegistrationController.class,
        PublicRegistrationBookingController.class
})
public class PublicRegistrationApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PublicRegistrationApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMultipart(MultipartException e) {
        logger.warn("public registration multipart error: {}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.69cf5090aeb8")));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException e) {
        logger.warn("public registration runtime error: {}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception e) {
        logger.error("public registration unexpected error", e);
        return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.9a3b4594bb0a")));
    }
}
