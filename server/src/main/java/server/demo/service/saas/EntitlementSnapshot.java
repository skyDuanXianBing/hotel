package server.demo.service.saas;

import server.demo.enums.SaasFeatureType;

import java.util.List;

/**
 * 订阅成交时冻结的权益快照（序列化为 JSON 存入 saas_subscription.entitlement_snapshot_json）。
 * 运行时所有权益判定只读快照，不回查套餐模板，改模板/改价不影响存量订阅。
 */
public record EntitlementSnapshot(List<Entry> features) {

    public record Entry(String featureCode, SaasFeatureType type, Long limit) {
    }

    public Entry find(String featureCode) {
        if (features == null || featureCode == null) {
            return null;
        }
        return features.stream()
                .filter(e -> featureCode.equals(e.featureCode()))
                .findFirst()
                .orElse(null);
    }
}
