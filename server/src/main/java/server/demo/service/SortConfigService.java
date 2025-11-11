package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.entity.SortConfig;
import server.demo.repository.SortConfigRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SortConfigService {

    @Autowired
    private SortConfigRepository sortConfigRepository;

    /**
     * 获取指定类型的排序配置
     */
    public List<SortConfig> getSortConfigs(Long userId, String sortType) {
        return sortConfigRepository.findByUserIdAndSortTypeOrderBySortOrderAsc(userId, sortType);
    }

    /**
     * 获取指定类型的排序映射(entityId -> sortOrder)
     */
    public Map<Long, Integer> getSortOrderMap(Long userId, String sortType) {
        List<SortConfig> configs = getSortConfigs(userId, sortType);
        return configs.stream()
                .collect(Collectors.toMap(
                        SortConfig::getEntityId,
                        SortConfig::getSortOrder
                ));
    }

    /**
     * 批量更新排序配置
     * @param userId 用户ID
     * @param sortType 排序类型
     * @param entityIds 实体ID列表(按排序顺序)
     */
    @Transactional
    public void updateSortOrders(Long userId, String sortType, List<Long> entityIds) {
        // 删除旧的排序配置
        sortConfigRepository.deleteByUserIdAndSortType(userId, sortType);

        // 创建新的排序配置
        for (int i = 0; i < entityIds.size(); i++) {
            SortConfig config = new SortConfig(userId, sortType, entityIds.get(i), i);
            sortConfigRepository.save(config);
        }
    }

    /**
     * 更新单个实体的排序
     */
    @Transactional
    public SortConfig updateSortOrder(Long userId, String sortType, Long entityId, Integer sortOrder) {
        SortConfig config = sortConfigRepository
                .findByUserIdAndSortTypeAndEntityId(userId, sortType, entityId)
                .orElse(new SortConfig(userId, sortType, entityId, sortOrder));

        config.setSortOrder(sortOrder);
        return sortConfigRepository.save(config);
    }

    /**
     * 删除指定类型的所有排序配置
     */
    @Transactional
    public void deleteSortConfigs(Long userId, String sortType) {
        sortConfigRepository.deleteByUserIdAndSortType(userId, sortType);
    }

    /**
     * 删除指定实体的排序配置
     */
    @Transactional
    public void deleteSortConfig(Long userId, String sortType, Long entityId) {
        sortConfigRepository.findByUserIdAndSortTypeAndEntityId(userId, sortType, entityId)
                .ifPresent(sortConfigRepository::delete);
    }
}
