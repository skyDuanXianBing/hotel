package server.demo.enums;

public enum IndependentSitePaymentProvider {
    SIMULATED,
    /** Stripe 真实支付：PaymentIntent 收卡 + webhook 验签驱动确认，/confirm 端点对其保持 422。 */
    STRIPE
}
