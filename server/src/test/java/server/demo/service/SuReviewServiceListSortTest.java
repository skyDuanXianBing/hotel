package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import server.demo.dto.ReviewDtos;
import server.demo.entity.ChannelReview;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评论列表排序修复的聚焦测试：排序改由 Specification 的 Criteria orderBy 表达，
 * 分页不再携带 nullsLast 的 Sort（避免 Criteria 路径抛 UnsupportedOperationException），
 * 排序语义保持 receivedAt DESC NULLS LAST, id DESC。
 */
class SuReviewServiceListSortTest {

    private ChannelReviewRepository reviewRepository;
    private SuReviewService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        reviewRepository = Mockito.mock(ChannelReviewRepository.class);
        PermissionService permissionService = Mockito.mock(PermissionService.class);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        service = new SuReviewService(
                reviewRepository,
                Mockito.mock(ChannelReviewActionRepository.class),
                Mockito.mock(ReservationRepository.class),
                Mockito.mock(SuReviewHotelOwnershipValidator.class),
                permissionService,
                new ReviewEligibilityService(),
                new AirbnbGuestReviewValidator(),
                Mockito.mock(ChannelReviewActionCoordinator.class),
                Mockito.mock(SuReviewWebhookMappingValidator.class),
                new SuReviewPayloadMapper(objectMapper),
                Mockito.mock(SuReviewClient.class),
                Mockito.mock(SuApiClient.class),
                objectMapper
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listReviewsQueriesWithUnsortedPageableAndCriteriaOrderSpecification() {
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        ReviewDtos.PageResponse response = service.listReviews(10L, 1L, 0, 20, "ALL", null, null, null);

        assertEquals(0, response.page());
        assertEquals(0, response.totalElements());

        // 分页不再携带 Sort（nullsLast 走 Criteria 路径会抛 UnsupportedOperationException）
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Specification<ChannelReview>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(reviewRepository).findAll(specCaptor.capture(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertTrue(pageable.getSort().isUnsorted());
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());

        Specification<ChannelReview> specification = specCaptor.getValue();

        // 1) 普通查询：应通过 selectCase 显式排序且不抛异常
        CriteriaMocks select = CriteriaMocks.forResultType(ChannelReview.class);
        specification.toPredicate(select.root, select.query, select.cb);
        verify(select.query).orderBy(select.caseAsc, select.receivedAtDesc, select.idDesc);
        verify(select.caseExpression).when(select.receivedAtIsNull, 1);
        verify(select.caseExpression).otherwise(0);

        // 2) count 查询（resultType 为 Long）：不得设置 orderBy
        CriteriaMocks count = CriteriaMocks.forResultType(Long.class);
        specification.toPredicate(count.root, count.query, count.cb);
        boolean orderByInvoked = Mockito.mockingDetails(count.query).getInvocations().stream()
                .anyMatch(invocation -> "orderBy".equals(invocation.getMethod().getName()));
        assertFalse(orderByInvoked);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class CriteriaMocks {
        private final Root<ChannelReview> root = Mockito.mock(Root.class);
        private final CriteriaQuery query = Mockito.mock(CriteriaQuery.class);
        private final CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);
        private final Path receivedAtPath = Mockito.mock(Path.class);
        private final Path idPath = Mockito.mock(Path.class);
        private final Predicate receivedAtIsNull = Mockito.mock(Predicate.class);
        private final CriteriaBuilder.Case caseExpression = Mockito.mock(CriteriaBuilder.Case.class);
        private final Expression caseResult = Mockito.mock(Expression.class);
        private final Order caseAsc = Mockito.mock(Order.class);
        private final Order receivedAtDesc = Mockito.mock(Order.class);
        private final Order idDesc = Mockito.mock(Order.class);

        private CriteriaMocks(Class<?> resultType) {
            when(query.getResultType()).thenReturn((Class) resultType);
            when(root.get("receivedAt")).thenReturn(receivedAtPath);
            when(root.get("id")).thenReturn(idPath);
            when(receivedAtPath.isNull()).thenReturn(receivedAtIsNull);
            when(cb.selectCase()).thenReturn(caseExpression);
            when(caseExpression.when(receivedAtIsNull, 1)).thenReturn(caseExpression);
            when(caseExpression.otherwise(0)).thenReturn(caseResult);
            when(cb.asc(caseResult)).thenReturn(caseAsc);
            when(cb.desc(receivedAtPath)).thenReturn(receivedAtDesc);
            when(cb.desc(idPath)).thenReturn(idDesc);
        }

        private static CriteriaMocks forResultType(Class<?> resultType) {
            return new CriteriaMocks(resultType);
        }
    }
}
