package server.demo.dto;

import server.demo.i18n.ApiMessages;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private String messageKey;
    private Object[] messageParams;
    private T data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, String messageKey, Object[] messageParams, T data) {
        this.success = success;
        this.message = message;
        this.messageKey = messageKey;
        this.messageParams = messageParams;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return keyedSuccess("api.common.success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> keyedSuccess(String messageKey, T data, Object... messageParams) {
        Object[] params = messageParams == null ? new Object[0] : messageParams;
        return new ApiResponse<>(true, ApiMessages.get(messageKey, params), messageKey, params, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    public static <T> ApiResponse<T> keyedError(String messageKey, Object... messageParams) {
        Object[] params = messageParams == null ? new Object[0] : messageParams;
        return new ApiResponse<>(false, ApiMessages.get(messageKey, params), messageKey, params, null);
    }

    public static <T> ApiResponse<T> keyedError(String messageKey, T data, Object... messageParams) {
        Object[] params = messageParams == null ? new Object[0] : messageParams;
        return new ApiResponse<>(false, ApiMessages.get(messageKey, params), messageKey, params, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Object[] getMessageParams() {
        return messageParams;
    }

    public void setMessageParams(Object[] messageParams) {
        this.messageParams = messageParams;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
