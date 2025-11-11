package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.PricePlan;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricePlanRepository extends JpaRepository<PricePlan, Long> {

    // 根据用户ID查找所有价格计划
    List<PricePlan> findByUserIdOrderByName(Long userId);

    // 根据用户ID和价格计划ID查找
    Optional<PricePlan> findByIdAndUserId(Long id, Long userId);

    // 根据用户ID和名称查找
    Optional<PricePlan> findByUserIdAndName(Long userId, String name);

    // 检查用户是否已有该名称的价格计划
    boolean existsByUserIdAndName(Long userId, String name);

    // 根据用户ID和名称查找(排除指定ID)
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PricePlan p WHERE p.user.id = :userId AND p.name = :name AND p.id != :id")
    boolean existsByUserIdAndNameAndIdNot(@Param("userId") Long userId, @Param("name") String name, @Param("id") Long id);

    // 查找所有独立类型的价格计划
    @Query("SELECT p FROM PricePlan p WHERE p.user.id = :userId AND p.derivationType = 'independent' ORDER BY p.name")
    List<PricePlan> findIndependentPlansByUserId(@Param("userId") Long userId);
}
