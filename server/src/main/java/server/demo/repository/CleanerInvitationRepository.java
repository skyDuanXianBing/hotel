package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.CleanerInvitation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 保洁员邀请Repository
 */
@Repository
public interface CleanerInvitationRepository extends JpaRepository<CleanerInvitation, Long> {

    /**
     * 根据token查找邀请
     */
    Optional<CleanerInvitation> findByToken(String token);

    /**
     * 根据邮箱查找待处理的邀请
     */
    List<CleanerInvitation> findByEmailAndStatus(String email, String status);

    /**
     * 查找过期的邀请
     */
    List<CleanerInvitation> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiresAt);
}
