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
        return ResponseEntity.ok(ApiResponse.error("\u4e0a\u4f20\u5931\u8d25\uff1a\u8bf7\u786e\u8ba4\u9009\u62e9\u4e86\u6587\u4ef6\uff0c\u5e76\u91cd\u8bd5"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException e) {
        logger.warn("public registration runtime error: {}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception e) {
        logger.error("public registration unexpected error", e);
        return ResponseEntity.ok(ApiResponse.error("\u7cfb\u7edf\u9519\u8bef\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5"));
    }
}
