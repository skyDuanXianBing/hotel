package server.demo.service.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.enums.SaasFeatureType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CapacityUsageService：按订阅快照组装 CAPACITY 权益的实时用量。
 * 策略注册：容量计数器以 List&lt;CapacityCounter&gt; 注入并注册成 Map，
 * 未注册计数器的容量权益跳过；BOOLEAN/QUOTA 不产生容量用量。
 */
class CapacityUsageServiceTest {

    private static final long STORE_ID = 7L;

    private CapacityCounter roomCountCounter;
    private CapacityUsageService service;

    @BeforeEach
    void setUp() {
        roomCountCounter = Mockito.mock(CapacityCounter.class);
        Mockito.when(roomCountCounter.featureCode()).thenReturn("room_count");
        service = new CapacityUsageService(List.of(roomCountCounter));
    }

    @Test
    void listCapacityUsages_capacityEntry_returnsRealTimeCount() {
        EntitlementSnapshot snapshot = new EntitlementSnapshot(List.of(
                new EntitlementSnapshot.Entry("independent_website", SaasFeatureType.BOOLEAN, null),
                new EntitlementSnapshot.Entry("ai_website_gen", SaasFeatureType.QUOTA, 50L),
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L)));
        Mockito.when(roomCountCounter.count(STORE_ID)).thenReturn(15L);

        List<CapacityUsage> usages = service.listCapacityUsages(STORE_ID, snapshot);

        // 仅 CAPACITY 权益出用量视图（BOOLEAN/QUOTA 跳过）；超量场景原样返回 15/10
        assertEquals(1, usages.size());
        CapacityUsage usage = usages.get(0);
        assertEquals("room_count", usage.featureCode());
        assertEquals(10L, usage.limit());
        assertEquals(15L, usage.used());
    }

    @Test
    void listCapacityUsages_unlimitedLimit_keepsNullLimit() {
        EntitlementSnapshot snapshot = new EntitlementSnapshot(List.of(
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, null)));
        Mockito.when(roomCountCounter.count(STORE_ID)).thenReturn(3L);

        List<CapacityUsage> usages = service.listCapacityUsages(STORE_ID, snapshot);

        assertEquals(1, usages.size());
        assertEquals(null, usages.get(0).limit());
        assertEquals(3L, usages.get(0).used());
    }

    @Test
    void listCapacityUsages_unregisteredCapacityFeature_skipped() {
        EntitlementSnapshot snapshot = new EntitlementSnapshot(List.of(
                new EntitlementSnapshot.Entry("parking_slots", SaasFeatureType.CAPACITY, 5L),
                new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L)));
        Mockito.when(roomCountCounter.count(STORE_ID)).thenReturn(8L);

        List<CapacityUsage> usages = service.listCapacityUsages(STORE_ID, snapshot);

        // 未注册计数器的容量权益不阻断、不出视图（前端回退为仅展示上限）
        assertEquals(1, usages.size());
        assertEquals("room_count", usages.get(0).featureCode());
    }

    @Test
    void listCapacityUsages_nullOrEmptySnapshot_returnsEmpty() {
        assertTrue(service.listCapacityUsages(STORE_ID, null).isEmpty());
        assertTrue(service.listCapacityUsages(STORE_ID, new EntitlementSnapshot(null)).isEmpty());
        assertTrue(service.listCapacityUsages(STORE_ID, new EntitlementSnapshot(List.of())).isEmpty());
        // 构造期注册 featureCode 之外，不产生任何计数调用
        Mockito.verify(roomCountCounter, Mockito.never()).count(Mockito.any());
    }
}
