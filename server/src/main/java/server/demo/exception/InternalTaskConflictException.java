package server.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InternalTaskConflictException extends RuntimeException {
    public InternalTaskConflictException(String message) { super(message); }
}
