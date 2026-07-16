package server.demo.controller.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import server.demo.controller.ManagedOperationSettlementController;
import server.demo.dto.ApiResponse;
import server.demo.exception.ManagedOperationValidationException;

@RestControllerAdvice(assignableTypes = ManagedOperationSettlementController.class)
public class ManagedOperationExceptionHandler {
    @ExceptionHandler(ManagedOperationValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ManagedOperationValidationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("上传文件超过服务器限制"));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("请求格式错误，请确认已上传 Airbnb、Booking 文件且 request 为有效 JSON"));
    }
}
