package server.demo.exception;

public class ManagedOperationValidationException extends RuntimeException {
    public ManagedOperationValidationException(String message) {
        super(message);
    }

    public ManagedOperationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
