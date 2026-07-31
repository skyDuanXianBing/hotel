package server.demo.service.saas;

/**
 * 容量型权益的实时用量计数器（策略接口）。
 * 每种 CAPACITY 权益对应一个实现 bean，由 {@link CapacityUsageService} 按 featureCode 注册成 Map；
 * 将来新增容量权益只需新增一个实现类，无需改动组装逻辑（避免 if-else 蔓延）。
 */
public interface CapacityCounter {

    /** 绑定的容量权益 featureCode（与功能字典一致）。 */
    String featureCode();

    /** 该门店当前的实时用量（如房间总数）。 */
    long count(Long storeId);
}
