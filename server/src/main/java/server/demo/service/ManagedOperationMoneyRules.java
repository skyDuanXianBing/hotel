package server.demo.service;

import server.demo.exception.ManagedOperationValidationException;

import java.math.BigDecimal;

import server.demo.i18n.ApiMessages;
public final class ManagedOperationMoneyRules {
    private ManagedOperationMoneyRules() {}

    public static boolean isWholeYen(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }

    public static void requireWholeYen(BigDecimal value, String field) {
        if (!isWholeYen(value)) {
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.06723280266a"));
        }
    }
}
