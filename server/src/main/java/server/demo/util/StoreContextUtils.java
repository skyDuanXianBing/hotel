package server.demo.util;

import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.exception.StoreAccessDeniedException;

/**
 * 统一获取当前请求的门店/用户上下文，避免控制器与服务重复代码。
 */
public final class StoreContextUtils {
    private StoreContextUtils() {
    }

    public static StoreContext requireContext() {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || context.getStoreId() == null) {
            throw new StoreAccessDeniedException("未获取到有效的门店上下文");
        }
        return context;
    }

    public static Long requireStoreId() {
        return requireContext().getStoreId();
    }

    public static Long requireUserId() {
        return requireContext().getUserId();
    }
}
