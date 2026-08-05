package server.demo.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SuConfigProxyController 本地 mock 白名单聚焦测试：三渠道 channelId 放行，未知 id 拒绝。
 */
class SuConfigProxyControllerLocalMockChannelIdTest {

    private final SuConfigProxyController controller = new SuConfigProxyController(null, null);

    @Test
    void shouldAllowAllFiveCatalogChannelIds() {
        assertTrue(controller.isAllowedLocalMockChannelId("19"));
        assertTrue(controller.isAllowedLocalMockChannelId("244"));
        assertTrue(controller.isAllowedLocalMockChannelId("9"));
        assertTrue(controller.isAllowedLocalMockChannelId("339"));
        assertTrue(controller.isAllowedLocalMockChannelId("189"));
    }

    @Test
    void shouldRejectUnknownChannelIds() {
        assertFalse(controller.isAllowedLocalMockChannelId(null));
        assertFalse(controller.isAllowedLocalMockChannelId(""));
        assertFalse(controller.isAllowedLocalMockChannelId("150"));
        assertFalse(controller.isAllowedLocalMockChannelId("253"));
        assertFalse(controller.isAllowedLocalMockChannelId("abc"));
    }
}
