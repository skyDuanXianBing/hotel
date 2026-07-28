package server.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RegistrationReviewConflictException extends RuntimeException {
    public RegistrationReviewConflictException(String message) {
        super(message);
    }
}
