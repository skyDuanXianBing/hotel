package server.demo.enums;

import server.demo.i18n.ApiMessages;

/**
 * 权限操作枚举
 */
public enum PermissionAction {
    // 住宿管理相关
    VIEW_ROOM_STATUS("api.t.7a08c4e6872f"),
    EDIT_ROOM_STATUS("api.t.b62808fa7740"),
    VIEW_ROOM_OPERATION_LOG("api.t.41139454eb58"),
    VIEW_ROOM_INFO("api.t.bbee6fe9575c"),
    ROOM_SHARE("api.t.2c6d7cf0860f"),
    VIEW_ROOM_PRICE("api.t.d178eb6d5791"),
    EDIT_ROOM_PRICE("api.t.ec3e9b56c08f"),
    VIEW_PRICE_LOG("api.t.8ec06723ba9a"),
    BATCH_CHANGE_PRICE("api.t.fc6422250a91"),
    BREAKFAST_PACKAGE("api.t.a0c240ca72ed"),
    RESERVATION_CALENDAR("api.t.2074f48abaec"),
    TASK_LIST("api.t.cfd7488ce6a8"),
    CREATE_INTERNAL_TASK("api.t.8323d0c53fc2"),

    // 订单管理相关
    VIEW_ORDERS("api.t.86940ce545a0"),
    CREATE_ORDER("api.t.5ac84906d9ba"),
    MODIFY_ORDER("api.t.89e5b08e91f0"),
    DELETE_ORDER("api.t.199a0795dbb9"),
    CANCEL_ORDER("api.t.3eedd32d80a7"),

    // 渠道相关
    VIEW_CHANNELS("api.t.f1a96c3bfdad"),
    MANAGE_CHANNELS("api.t.1a577e2e2c99"),

    // 评价管理相关
    VIEW("api.t.c5b0c008d168"),
    REPLY("api.t.05b5839998bb"),
    REVIEW_GUEST("api.t.50aecd9806ae"),
    SYNC("api.t.d6f34176976e"),

    // 客户管理相关
    VIEW_CUSTOMERS("api.t.15b14c529d53"),
    MANAGE_CUSTOMERS("api.t.10765cc521ac"),

    // 统计分析相关
    VIEW_STATS("api.t.d353ff376551"),
    EXPORT_STATS("api.t.9fe52643a711"),

    // 设置相关
    VIEW_SETTINGS("api.t.c448c3067ce0"),
    MODIFY_SETTINGS("api.t.18e1aab00ec6"),
    MODIFY_STORE_SETTINGS("api.t.22a15668195e"),
    MANAGE_EMPLOYEE_ACCOUNTS("api.t.2f0a670a168c"),
    MANAGE_PAYMENT_METHODS("api.t.4a8ef10ce718"),

    // 数据中心相关
    VIEW_DATA("api.t.8afda7e67890"),
    EXPORT_DATA("api.t.437d69f3e30b"),

    // 敏感权限
    VIEW_FINANCIAL_DATA("api.t.f0b445463be8"),
    DELETE_IMPORTANT_DATA("api.t.49c530efe87d");

    private final String displayNameKey;

    PermissionAction(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    public String getDisplayName() {
        return ApiMessages.get(displayNameKey);
    }
}
