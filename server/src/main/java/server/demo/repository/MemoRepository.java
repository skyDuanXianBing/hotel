package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Memo;

import java.util.Optional;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {

    /**
     * 根据用户ID查询备忘录
     * 每个用户只有一条备忘录记录
     */
    Optional<Memo> findByUserId(Long userId);

    /**
     * 检查用户是否已有备忘录
     */
    boolean existsByUserId(Long userId);
}
