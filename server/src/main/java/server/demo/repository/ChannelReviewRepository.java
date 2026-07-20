package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.ChannelReview;

import java.util.Optional;

public interface ChannelReviewRepository
        extends JpaRepository<ChannelReview, Long>, JpaSpecificationExecutor<ChannelReview> {

    Optional<ChannelReview> findByStoreIdAndId(Long storeId, Long id);

    Optional<ChannelReview> findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
            Long storeId,
            Integer suChannelId,
            String channelReviewId,
            String reviewType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM ChannelReview r
            WHERE r.storeId = :storeId
              AND r.id = :id
            """)
    Optional<ChannelReview> findForUpdateByStoreIdAndId(
            @Param("storeId") Long storeId,
            @Param("id") Long id
    );
}
