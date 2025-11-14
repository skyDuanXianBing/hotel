package server.demo.entity.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.base.StoreScopedEntity;

/**
 * 在持久化/更新前自动写入门店ID，避免忘记赋值。
 */
public class StoreScopedEntityListener {

    @PrePersist
    public void prePersist(StoreScopedEntity entity) {
        applyStoreContext(entity);
    }

    @PreUpdate
    public void preUpdate(StoreScopedEntity entity) {
        if (entity.getStoreId() == null) {
            applyStoreContext(entity);
        }
    }

    private void applyStoreContext(StoreScopedEntity entity) {
        if (entity.getStoreId() != null) {
            return;
        }
        StoreContext context = StoreContextHolder.getContext();
        if (context != null && context.getStoreId() != null) {
            entity.setStoreId(context.getStoreId());
        }
    }
}
