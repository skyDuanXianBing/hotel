package server.demo.repository.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.admin.AdminUser;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);
}
