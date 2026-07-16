package server.demo.interceptor;

import org.junit.jupiter.api.Test;
import server.demo.annotation.StoreScoped;
import server.demo.controller.ManagedOperationSettlementController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreContextInterceptorWarmupTest {
    @Test
    void managedOperationController_shouldExplicitlyDisableWarmup() {
        StoreScoped annotation = ManagedOperationSettlementController.class.getAnnotation(StoreScoped.class);
        assertFalse(annotation.warmupChannelPrices());
    }

    @Test
    void existingControllers_shouldKeepWarmupDefaultEnabled() throws Exception {
        assertTrue(StoreScoped.class.getMethod("warmupChannelPrices").getDefaultValue().equals(Boolean.TRUE));
    }
}
