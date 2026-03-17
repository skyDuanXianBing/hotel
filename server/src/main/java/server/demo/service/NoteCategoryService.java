package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.entity.NoteCategory;
import server.demo.repository.NoteCategoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * 记一笔分类 Service（门店级架构）
 */
@Service
public class NoteCategoryService {

    @Autowired
    private NoteCategoryRepository noteCategoryRepository;

    /**
     * 获取当前门店的所有分类（按显示顺序排序）
     */
    public List<NoteCategory> getAllCategories() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return noteCategoryRepository.findByStoreIdOrderByDisplayOrderAsc(storeId);
    }

    /**
     * 根据类型获取分类（按显示顺序排序）
     */
    public List<NoteCategory> getCategoriesByType(String type) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return noteCategoryRepository.findByStoreIdAndTypeOrderByDisplayOrderAsc(storeId, type);
    }

    /**
     * 根据ID获取分类
     */
    public Optional<NoteCategory> getCategoryById(Long id) {
        return noteCategoryRepository.findById(id);
    }

    /**
     * 创建分类（storeId由StoreScopedEntityListener自动注入）
     */
    @Transactional
    public NoteCategory createCategory(NoteCategory category) {
        return noteCategoryRepository.save(category);
    }

    /**
     * 批量创建分类
     */
    @Transactional
    public List<NoteCategory> createCategories(List<NoteCategory> categories) {
        return noteCategoryRepository.saveAll(categories);
    }

    /**
     * 更新分类
     */
    @Transactional
    public NoteCategory updateCategory(Long id, NoteCategory updates) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        NoteCategory category = noteCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        // 验证分类属于当前门店
        if (!storeId.equals(category.getStoreId())) {
            throw new RuntimeException("无权限修改此分类");
        }

        if (updates.getName() != null) {
            category.setName(updates.getName());
        }
        if (updates.getType() != null) {
            category.setType(updates.getType());
        }
        if (updates.getDisplayOrder() != null) {
            category.setDisplayOrder(updates.getDisplayOrder());
        }

        return noteCategoryRepository.save(category);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        NoteCategory category = noteCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        // 验证分类属于当前门店
        if (!storeId.equals(category.getStoreId())) {
            throw new RuntimeException("无权限删除此分类");
        }

        noteCategoryRepository.deleteById(id);
    }

    /**
     * 批量更新分类排序
     */
    @Transactional
    public List<NoteCategory> updateCategoriesOrder(List<NoteCategory> categories) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<NoteCategory> toUpdate = new java.util.ArrayList<>();

        // 验证所有分类都属于当前门店
        for (NoteCategory category : categories) {
            NoteCategory existing = noteCategoryRepository.findById(category.getId())
                    .orElseThrow(() -> new RuntimeException("分类不存在: " + category.getId()));

            if (!storeId.equals(existing.getStoreId())) {
                throw new RuntimeException("无权限修改此分类: " + category.getId());
            }

            existing.setDisplayOrder(category.getDisplayOrder());
            toUpdate.add(existing);
        }

        return noteCategoryRepository.saveAll(toUpdate);
    }
}
