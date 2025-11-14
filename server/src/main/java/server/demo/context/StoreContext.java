package server.demo.context;

/**
 * 保存当前请求的门店上下文信息。
 */
public class StoreContext {
    private final Long userId;
    private final Long storeId;
    private final String role;

    public StoreContext(Long userId, Long storeId, String role) {
        this.userId = userId;
        this.storeId = storeId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getRole() {
        return role;
    }
}
