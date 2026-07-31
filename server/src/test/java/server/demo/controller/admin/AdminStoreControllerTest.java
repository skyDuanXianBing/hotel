package server.demo.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.StoreSearchItem;
import server.demo.service.admin.AdminStoreService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P5 修复：GET /api/admin/stores/search 端点契约——透传 keyword 并包装命中列表。
 */
class AdminStoreControllerTest {

    private AdminStoreService adminStoreService;
    private AdminStoreController controller;

    @BeforeEach
    void setUp() {
        adminStoreService = Mockito.mock(AdminStoreService.class);
        controller = new AdminStoreController(adminStoreService);
    }

    @Test
    void search_delegatesKeywordAndWrapsItems() {
        when(adminStoreService.searchStores("云栖"))
                .thenReturn(List.of(new StoreSearchItem(3L, "云栖酒店")));

        ApiResponse<List<StoreSearchItem>> response = controller.searchStores("云栖");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(3L, response.getData().get(0).id());
        assertEquals("云栖酒店", response.getData().get(0).name());
        verify(adminStoreService).searchStores("云栖");
    }

    @Test
    void search_keywordOptional_nullPassedThrough() {
        when(adminStoreService.searchStores(null)).thenReturn(List.of());

        ApiResponse<List<StoreSearchItem>> response = controller.searchStores(null);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isEmpty());
        verify(adminStoreService).searchStores(null);
    }
}
