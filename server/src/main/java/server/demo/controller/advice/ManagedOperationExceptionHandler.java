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

import server.demo.i18n.ApiMessages;
@RestControllerAdvice(assignableTypes = ManagedOperationSettlementController.class)
public class ManagedOperationExceptionHandler {
    @ExceptionHandler(ManagedOperationValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ManagedOperationValidationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ApiMessages.get("api.t.81153184aefd")));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiMessages.get("api.t.296a9b037d86")));
    }
}
