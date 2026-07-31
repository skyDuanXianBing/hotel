package server.demo.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import server.demo.dto.admin.AdminDtos.StoreSearchItem;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端门店选择器搜索：keyword 归一化（空白 → null 不过滤）、limit 20、id+name 映射。
 * 名称模糊 / ID 精确匹配的 SQL 语义由仓储层 @Query 表达，此处锁定服务层契约。
 */
class AdminStoreServiceTest {

    private StoreRepository storeRepository;
    private AdminStoreService service;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        service = new AdminStoreService(storeRepository);
    }

    private Store store(long id, String name) {
        Store store = new Store();
        store.setId(id);
        store.setName(name);
        return store;
    }

    @Test
    void searchStores_keywordProvided_passesTrimmedKeywordAndLimit20() {
        when(storeRepository.searchByKeyword(eq("云栖"), any(Pageable.class)))
                .thenReturn(List.of(store(3L, "云栖酒店"), store(8L, "云栖别院")));

        List<StoreSearchItem> items = service.searchStores("  云栖  ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(storeRepository).searchByKeyword(eq("云栖"), pageableCaptor.capture());
        assertEquals(AdminStoreService.SEARCH_LIMIT, pageableCaptor.getValue().getPageSize());
        assertEquals(20, AdminStoreService.SEARCH_LIMIT);

        assertEquals(2, items.size());
        assertEquals(3L, items.get(0).id());
        assertEquals("云栖酒店", items.get(0).name());
        assertEquals(8L, items.get(1).id());
    }

    @Test
    void searchStores_blankKeyword_normalizedToNull_unfiltered() {
        when(storeRepository.searchByKeyword(eq(null), any(Pageable.class)))
                .thenReturn(List.of(store(1L, "首店")));

        List<StoreSearchItem> items = service.searchStores("   ");

        verify(storeRepository).searchByKeyword(eq(null), any(Pageable.class));
        assertEquals(1, items.size());
        assertEquals(1L, items.get(0).id());
    }

    @Test
    void searchStores_nullKeyword_normalizedToNull() {
        when(storeRepository.searchByKeyword(eq(null), any(Pageable.class)))
                .thenReturn(List.of());

        List<StoreSearchItem> items = service.searchStores(null);

        verify(storeRepository).searchByKeyword(eq(null), any(Pageable.class));
        assertTrue(items.isEmpty());
    }

    @Test
    void searchStores_numericKeyword_passedThrough_repositoryMatchesIdExactly() {
        // ID 精确匹配由仓储 @Query 的 CAST(s.id AS string) = :keyword 承担；服务层原样透传
        when(storeRepository.searchByKeyword(eq("42"), any(Pageable.class)))
                .thenReturn(List.of(store(42L, "湖滨店")));

        List<StoreSearchItem> items = service.searchStores("42");

        verify(storeRepository).searchByKeyword(eq("42"), any(Pageable.class));
        assertEquals(1, items.size());
        assertEquals(42L, items.get(0).id());
        assertEquals("湖滨店", items.get(0).name());
    }

    @Test
    void searchStores_noHit_returnsEmptyList() {
        when(storeRepository.searchByKeyword(eq("不存在"), any(Pageable.class)))
                .thenReturn(List.of());

        List<StoreSearchItem> items = service.searchStores("不存在");

        assertTrue(items.isEmpty());
    }
}
