package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.ChannelReviewAction;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChannelReviewActionRepository extends JpaRepository<ChannelReviewAction, Long> {

    Optional<ChannelReviewAction> findByStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    boolean existsByStoreIdAndReviewIdAndActionTypeAndStatusIn(
            Long storeId,
            Long reviewId,
            ReviewActionType actionType,
            Collection<ReviewActionStatus> statuses
    );

    List<ChannelReviewAction> findByStoreIdAndReviewIdOrderByCreatedAtDesc(Long storeId, Long reviewId);

    Optional<ChannelReviewAction> findFirstByStoreIdAndReviewIdAndActionTypeAndStatusInOrderByCreatedAtDesc(
            Long storeId,
            Long reviewId,
            ReviewActionType actionType,
            Collection<ReviewActionStatus> statuses
    );
}
