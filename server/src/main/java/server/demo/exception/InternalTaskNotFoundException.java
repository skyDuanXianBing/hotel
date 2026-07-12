package server.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InternalTaskNotFoundException extends RuntimeException {
    public InternalTaskNotFoundException(String message) { super(message); }
}
