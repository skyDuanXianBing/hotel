package server.demo.enums;

/**
 * 权限模块枚举
 */
public enum PermissionModule {
    ACCOMMODATION("住宿管理"),
    ORDER("订单管理"),
    CHANNEL("渠道"),
    CUSTOMER("客户管理"),
    STATISTICS("统计分析"),
    SETTINGS("设置"),
    DATA_CENTER("数据中心"),
    SENSITIVE("敏感权限");

    private final String displayName;

    PermissionModule(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
