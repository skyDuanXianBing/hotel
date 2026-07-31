package server.demo.service.saas;

import org.springframework.stereotype.Service;
import server.demo.enums.SaasFeatureType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 容量型权益用量组装：按订阅快照中的 CAPACITY 权益逐项调用已注册的 {@link CapacityCounter}
 * 取业务库实时 COUNT。未注册计数器的容量权益跳过（前端回退为仅展示上限）。
 */
@Service
public class CapacityUsageService {

    private final Map<String, CapacityCounter> countersByFeature;

    public CapacityUsageService(List<CapacityCounter> counters) {
        Map<String, CapacityCounter> byFeature = new LinkedHashMap<>();
        for (CapacityCounter counter : counters) {
            byFeature.put(counter.featureCode(), counter);
        }
        this.countersByFeature = Map.copyOf(byFeature);
    }

    /**
     * 快照中全部 CAPACITY 权益的实时用量。snapshot 为 null（无有效订阅）时返回空列表。
     */
    public List<CapacityUsage> listCapacityUsages(Long storeId, EntitlementSnapshot snapshot) {
        if (snapshot == null || snapshot.features() == null) {
            return List.of();
        }
        List<CapacityUsage> usages = new ArrayList<>();
        for (EntitlementSnapshot.Entry entry : snapshot.features()) {
            if (entry.type() != SaasFeatureType.CAPACITY) {
                continue;
            }
            CapacityCounter counter = countersByFeature.get(entry.featureCode());
            if (counter == null) {
                continue;
            }
            usages.add(new CapacityUsage(entry.featureCode(), entry.limit(), counter.count(storeId)));
        }
        return usages;
    }
}
