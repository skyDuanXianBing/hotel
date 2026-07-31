package server.demo.enums;

/**
 * SaaS 权益类型：
 * BOOLEAN  - 开关型，订阅快照中存在即拥有；
 * QUOTA    - 消耗型计次权益（如 AI 建站次数），需要预扣/返还；
 * CAPACITY - 容量型上限（如可存在房间数），不扣减不重置，实时 COUNT 对比。
 */
public enum SaasFeatureType {
    BOOLEAN,
    QUOTA,
    CAPACITY
}
