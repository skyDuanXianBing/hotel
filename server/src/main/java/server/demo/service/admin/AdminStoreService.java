package server.demo.service.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.admin.AdminDtos.StoreSearchItem;
import server.demo.repository.StoreRepository;

import java.util.List;

/**
 * 平台管理端门店查询（门店选择器远程搜索）。
 */
@Service
public class AdminStoreService {

    /** 选择器候选上限：名称模糊 + ID 精确匹配合计最多返回 20 条。 */
    static final int SEARCH_LIMIT = 20;

    private final StoreRepository storeRepository;

    public AdminStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * 门店搜索：名称模糊匹配 + ID 精确匹配，按 id 升序最多 {@value #SEARCH_LIMIT} 条；
     * keyword 空白时不过滤（返回 id 最小的前 20 家，供选择器初始下拉）。
     */
    @Transactional(readOnly = true)
    public List<StoreSearchItem> searchStores(String keyword) {
        String normalized = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        return storeRepository.searchByKeyword(normalized, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(store -> new StoreSearchItem(store.getId(), store.getName()))
                .toList();
    }
}
