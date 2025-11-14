package server.demo.controller;

import server.demo.util.StoreContextUtils;

/**
 * 提供获取当前请求门店/用户的便捷方法。
 */
public abstract class BaseStoreController {

    protected Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    protected Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }
}
