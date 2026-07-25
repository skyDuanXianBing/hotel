package server.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Stripe 最小货币单位换算：JPY/KRW 等零小数货币 ×1，其余 ×100。
 * 零小数清单取自 Stripe 文档的 zero-decimal currencies。
 */
public final class StripeCurrencyAmounts {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
    );

    private StripeCurrencyAmounts() {
    }

    /**
     * 服务端金额（scale=2）转 Stripe 最小货币单位整数。
     * 换算后出现非整数视为数据错误（报价链路不应产生），直接抛出。
     */
    public static long toMinorUnits(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        String currency = currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
        BigDecimal minor = ZERO_DECIMAL_CURRENCIES.contains(currency)
                ? amount
                : amount.movePointRight(2);
        return minor.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }
}
