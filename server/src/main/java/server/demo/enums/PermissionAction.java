package server.demo.enums;

/**
 * 权限操作枚举
 */
public enum PermissionAction {
    // 住宿管理相关
    VIEW_ROOM_STATUS("查看房态"),
    EDIT_ROOM_STATUS("修改房态"),
    VIEW_ROOM_OPERATION_LOG("查看房态操作日志"),
    VIEW_ROOM_INFO("查看房情表"),
    ROOM_SHARE("房态分享"),
    VIEW_ROOM_PRICE("查看房价"),
    EDIT_ROOM_PRICE("修改房价"),
    VIEW_PRICE_LOG("查看改价记录"),
    BATCH_CHANGE_PRICE("批量改价"),
    BREAKFAST_PACKAGE("餐食核销"),
    RESERVATION_CALENDAR("保洁日历"),
    TASK_LIST("任务列表"),
    CREATE_INTERNAL_TASK("创建其他任务"),

    // 订单管理相关
    VIEW_ORDERS("查看订单"),
    CREATE_ORDER("创建订单"),
    MODIFY_ORDER("修改订单"),
    DELETE_ORDER("删除订单"),
    CANCEL_ORDER("取消订单"),

    // 渠道相关
    VIEW_CHANNELS("查看渠道"),
    MANAGE_CHANNELS("管理渠道"),

    // 客户管理相关
    VIEW_CUSTOMERS("查看客户信息"),
    MANAGE_CUSTOMERS("编辑客户信息"),

    // 统计分析相关
    VIEW_STATS("查看统计数据"),
    EXPORT_STATS("导出报表"),

    // 设置相关
    VIEW_SETTINGS("查看设置"),
    MODIFY_SETTINGS("修改设置"),
    MODIFY_STORE_SETTINGS("修改门店设置"),
    MANAGE_EMPLOYEE_ACCOUNTS("管理员工账号"),
    MANAGE_PAYMENT_METHODS("管理支付方式"),

    // 数据中心相关
    VIEW_DATA("查看数据"),
    EXPORT_DATA("导出数据"),

    // 敏感权限
    VIEW_FINANCIAL_DATA("查看财务数据"),
    DELETE_IMPORTANT_DATA("删除重要数据");

    private final String displayName;

    PermissionAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
