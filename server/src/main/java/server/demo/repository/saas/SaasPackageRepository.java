package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.saas.SaasPackage;
import server.demo.enums.SaasPackageStatus;

import java.util.List;
import java.util.Optional;

public interface SaasPackageRepository extends JpaRepository<SaasPackage, Long> {

    List<SaasPackage> findByStatusOrderByPriceAsc(SaasPackageStatus status);

    /** 系统兜底套餐（is_system=1，V065 起由迁移唯一置位）：到期自动回退时按此查找。 */
    Optional<SaasPackage> findFirstByIsSystemTrueOrderByIdAsc();
}
