package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    List<User> findByNicknameContaining(String nickname);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.nickname LIKE %:keyword% OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query(value = "SELECT * FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL :days DAY)", nativeQuery = true)
    List<User> findRecentUsers(@Param("days") int days);

    // 账号管理相关查询
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email LIKE %:keyword% OR u.name LIKE %:keyword%")
    List<User> searchAccounts(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT u FROM User u WHERE u.isActive = :isActive")
    List<User> findByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE (:keyword IS NULL OR u.email LIKE %:keyword% OR u.name LIKE %:keyword%) AND (:roleId IS NULL OR :roleId IN (SELECT r.id FROM u.roles r)) AND (:isActive IS NULL OR u.isActive = :isActive)")
    List<User> findAccountsWithFilters(@Param("keyword") String keyword, @Param("roleId") Long roleId, @Param("isActive") Boolean isActive);
}