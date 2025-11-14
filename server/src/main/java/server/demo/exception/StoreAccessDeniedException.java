package server.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 当用户尝试访问未授权门店资源时抛出。
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class StoreAccessDeniedException extends RuntimeException {
    public StoreAccessDeniedException(String message) {
        super(message);
    }
}
