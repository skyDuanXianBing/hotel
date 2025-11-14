package server.demo.entity.base;

/**
 * 房态相关实体实现该接口以统一访问 storeId。
 */
public interface StoreScopedEntity {
    Long getStoreId();

    void setStoreId(Long storeId);
}
