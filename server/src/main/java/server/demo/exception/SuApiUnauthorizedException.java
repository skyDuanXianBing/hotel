package server.demo.exception;

/**
 * Su API 返回 401 Unauthorized 的统一异常。
 * <p>
 * 该异常用于触发上层的 Access Token 自动刷新并重试一次。
 */
public class SuApiUnauthorizedException extends RuntimeException {

    private final String actionName;

    public SuApiUnauthorizedException(String actionName, String message) {
        super(message);
        this.actionName = actionName;
    }

    public SuApiUnauthorizedException(String actionName, String message, Throwable cause) {
        super(message, cause);
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }
}

