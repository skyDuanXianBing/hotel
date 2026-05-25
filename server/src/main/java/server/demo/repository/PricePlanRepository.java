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

    List<PricePlan> findByStoreIdOrderByName(Long storeId);

    Optional<PricePlan> findByStoreIdAndId(Long storeId, Long id);

    Optional<PricePlan> findByStoreIdAndName(Long storeId, String name);

    List<PricePlan> findAllByStoreIdAndNameOrderByIdAsc(Long storeId, String name);

    boolean existsByStoreIdAndName(Long storeId, String name);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PricePlan p WHERE p.storeId = :storeId AND p.name = :name AND p.id != :id")
    boolean existsByStoreIdAndNameAndIdNot(@Param("storeId") Long storeId, @Param("name") String name, @Param("id") Long id);

    @Query("SELECT p FROM PricePlan p WHERE p.storeId = :storeId AND p.derivationType = 'independent' ORDER BY p.name")
    List<PricePlan> findIndependentPlansByStoreId(@Param("storeId") Long storeId);

    // legacy user-based APIs kept temporarily for migration
    @Deprecated
    List<PricePlan> findByUserIdOrderByName(Long userId);

    @Deprecated
    Optional<PricePlan> findByIdAndUserId(Long id, Long userId);

    @Deprecated
    Optional<PricePlan> findByUserIdAndName(Long userId, String name);

    @Deprecated
    boolean existsByUserIdAndName(Long userId, String name);

    @Deprecated
    boolean existsByUserIdAndNameAndIdNot(@Param("userId") Long userId, @Param("name") String name, @Param("id") Long id);

    @Deprecated
    @Query("SELECT p FROM PricePlan p WHERE p.user.id = :userId AND p.derivationType = 'independent' ORDER BY p.name")
    List<PricePlan> findIndependentPlansByUserId(@Param("userId") Long userId);
}
