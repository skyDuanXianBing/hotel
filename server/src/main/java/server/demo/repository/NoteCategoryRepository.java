package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.NoteCategory;

import java.util.List;

/**
 * 记一笔分类 Repository（门店级架构）
 */
@Repository
public interface NoteCategoryRepository extends JpaRepository<NoteCategory, Long> {

    /**
     * 根据门店ID查找所有分类
     */
    List<NoteCategory> findByStoreId(Long storeId);

    /**
     * 根据门店ID和类型查找分类
     */
    List<NoteCategory> findByStoreIdAndType(Long storeId, String type);

    /**
     * 根据门店ID查找所有分类，按显示顺序排序
     */
    List<NoteCategory> findByStoreIdOrderByDisplayOrderAsc(Long storeId);

    /**
     * 根据门店ID和类型查找分类，按显示顺序排序
     */
    List<NoteCategory> findByStoreIdAndTypeOrderByDisplayOrderAsc(Long storeId, String type);
}
