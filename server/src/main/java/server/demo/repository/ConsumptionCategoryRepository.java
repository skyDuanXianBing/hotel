package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ConsumptionCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionCategoryRepository extends JpaRepository<ConsumptionCategory, Long> {

    /**
     * 根据用户ID查找所有分类
     */
    List<ConsumptionCategory> findByUserId(Long userId);

    /**
     * 根据用户ID和分类名称查找分类
     */
    Optional<ConsumptionCategory> findByUserIdAndName(Long userId, String name);
}
