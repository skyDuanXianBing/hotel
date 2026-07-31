package server.demo.enums;

import server.demo.i18n.ApiMessages;

/**
 * 权限模块枚举
 */
public enum PermissionModule {
    ACCOMMODATION("api.t.8bfafb127a3c"),
    ORDER("api.t.26d8e2fa2315"),
    CHANNEL("api.t.c152be9f5040"),
    REVIEW("api.t.b1dee6e37240"),
    CUSTOMER("api.t.88c9f74936f0"),
    STATISTICS("api.t.29ac4b91707c"),
    SETTINGS("api.t.7debf9cb0372"),
    DATA_CENTER("api.t.189979277dde"),
    SENSITIVE("api.t.c68bec147e83");

    private final String displayNameKey;

    PermissionModule(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    public String getDisplayName() {
        return ApiMessages.get(displayNameKey);
    }
}
