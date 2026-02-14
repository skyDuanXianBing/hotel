package server.demo.exception;

/**
 * 权限不足异常（用于统一返回 403）。
 */
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}

