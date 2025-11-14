package server.demo.context;

/**
 * 基于 ThreadLocal 保存 StoreContext，确保同一线程内可随时读取。
 */
public final class StoreContextHolder {
    private static final ThreadLocal<StoreContext> CONTEXT = new ThreadLocal<>();

    private StoreContextHolder() {
    }

    public static void setContext(StoreContext context) {
        CONTEXT.set(context);
    }

    public static StoreContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
