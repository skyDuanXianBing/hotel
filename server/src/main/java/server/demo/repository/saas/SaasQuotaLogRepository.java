package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.saas.SaasQuotaLog;

public interface SaasQuotaLogRepository extends JpaRepository<SaasQuotaLog, Long> {
}
