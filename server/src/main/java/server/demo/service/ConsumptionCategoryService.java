package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.entity.ConsumptionCategory;
import server.demo.entity.ConsumptionItem;
import server.demo.repository.ConsumptionCategoryRepository;
import server.demo.repository.ConsumptionItemRepository;

import java.util.List;

@Service
public class ConsumptionCategoryService {

    @Autowired
    private ConsumptionCategoryRepository categoryRepository;

    @Autowired
    private ConsumptionItemRepository itemRepository;

    /**
     * 获取当前门店的所有分类（门店级架构）
     */
    public List<ConsumptionCategory> getAll() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<ConsumptionCategory> categories = categoryRepository.findByStoreId(storeId);

        // 更新每个分类的消费项数量
        for (ConsumptionCategory category : categories) {
            List<ConsumptionItem> items = itemRepository.findByStoreIdAndCategory(storeId, category.getName());
            category.setCount(items.size());
        }

        return categories;
    }

    /**
     * @deprecated 已废弃，使用门店级架构
     */
    @Deprecated
    public List<ConsumptionCategory> getAllByUserId(Long userId) {
        List<ConsumptionCategory> categories = categoryRepository.findByUserId(userId);

        // 更新每个分类的消费项数量
        for (ConsumptionCategory category : categories) {
            List<ConsumptionItem> items = itemRepository.findByUserIdAndCategory(userId, category.getName());
            category.setCount(items.size());
        }

        return categories;
    }

    /**
     * 根据ID获取分类
     */
    public ConsumptionCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
    }

    /**
     * 根据名称获取分类（门店级架构）
     */
    public ConsumptionCategory getByName(String name) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return categoryRepository.findByStoreIdAndName(storeId, name)
                .orElse(null);
    }

    /**
     * @deprecated 已废弃，使用门店级架构
     */
    @Deprecated
    public ConsumptionCategory getByName(Long userId, String name) {
        return categoryRepository.findByUserIdAndName(userId, name)
                .orElse(null);
    }

    /**
     * 创建分类（storeId由StoreScopedEntityListener自动注入）
     */
    @Transactional
    public ConsumptionCategory create(ConsumptionCategory category) {
        // 检查同名分类是否存在
        ConsumptionCategory existing = getByName(category.getName());
        if (existing != null) {
            throw new RuntimeException("分类名称已存在");
        }
        return categoryRepository.save(category);
    }

    /**
     * 更新分类
     */
    @Transactional
    public ConsumptionCategory update(Long id, ConsumptionCategory updates) {
        ConsumptionCategory category = getById(id);

        if (updates.getName() != null && !updates.getName().equals(category.getName())) {
            // 检查新名称是否已存在
            ConsumptionCategory existing = getByName(updates.getName());
            if (existing != null) {
                throw new RuntimeException("分类名称已存在");
            }
            category.setName(updates.getName());
        }

        if (updates.getDescription() != null) {
            category.setDescription(updates.getDescription());
        }

        return categoryRepository.save(category);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void delete(Long id) {
        ConsumptionCategory category = getById(id);
        Long storeId = StoreContextHolder.getContext().getStoreId();

        // 检查该分类下是否有消费项
        List<ConsumptionItem> items = itemRepository.findByStoreIdAndCategory(
                storeId, category.getName());
        if (!items.isEmpty()) {
            throw new RuntimeException("该分类下存在消费项,无法删除");
        }

        categoryRepository.deleteById(id);
    }
}
