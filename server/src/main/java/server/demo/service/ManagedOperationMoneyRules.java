package server.demo.service;

import server.demo.exception.ManagedOperationValidationException;

import java.math.BigDecimal;

public final class ManagedOperationMoneyRules {
    private ManagedOperationMoneyRules() {}

    public static boolean isWholeYen(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }

    public static void requireWholeYen(BigDecimal value, String field) {
        if (!isWholeYen(value)) {
            throw new ManagedOperationValidationException(field + "必须是整日元金额，不能包含非零小数");
        }
    }
}
