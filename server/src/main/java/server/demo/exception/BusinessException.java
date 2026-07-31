package server.demo.exception;

import org.springframework.http.HttpStatus;
import server.demo.i18n.ApiMessages;

/**
 * Business error carrying a MessageSource key (and optional args) for API localization.
 */
public class BusinessException extends RuntimeException {

    private final String messageKey;
    private final Object[] messageParams;
    private final HttpStatus status;

    public BusinessException(String messageKey, Object... messageParams) {
        this(HttpStatus.BAD_REQUEST, messageKey, messageParams);
    }

    public BusinessException(HttpStatus status, String messageKey, Object... messageParams) {
        super(messageKey);
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
        this.messageKey = messageKey;
        this.messageParams = messageParams == null ? new Object[0] : messageParams.clone();
    }

    public static BusinessException of(String messageKey, Object... messageParams) {
        return new BusinessException(messageKey, messageParams);
    }

    public static BusinessException of(HttpStatus status, String messageKey, Object... messageParams) {
        return new BusinessException(status, messageKey, messageParams);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageParams() {
        return messageParams.clone();
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return ApiMessages.get(messageKey, messageParams);
    }
}
