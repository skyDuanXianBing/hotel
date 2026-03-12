package server.demo.exception;

public class SuPropertyDeleteFailedException extends RuntimeException {
    private final String errorCode;

    public SuPropertyDeleteFailedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

