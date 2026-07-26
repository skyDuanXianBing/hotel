package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.util.List;
import java.util.Optional;

/**
 * Review 功能使用的 Su hotel_id 唯一归属校验。
 * <p>
 * Store 表当前没有唯一约束，因此所有 Review 外部入口都必须 fail-closed：
 * hotel_id 必须恰好匹配一条 Store，并且该 Store 必须是当前门店。
 */
@Component
public class SuReviewHotelOwnershipValidator {

    private final StoreRepository storeRepository;

    public SuReviewHotelOwnershipValidator(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Optional<Store> resolveUniqueOwner(String rawHotelId) {
        String hotelId = SuHotelIdUtil.normalize(rawHotelId);
        if (hotelId == null) {
            return Optional.empty();
        }
        List<Store> matches = storeRepository.findAllBySuHotelIdOrderByIdAsc(hotelId);
        if (matches == null || matches.size() != 1) {
            return Optional.empty();
        }
        Store owner = matches.get(0);
        if (owner == null
                || owner.getId() == null
                || !hotelId.equals(SuHotelIdUtil.normalize(owner.getSuHotelId()))) {
            return Optional.empty();
        }
        return Optional.of(owner);
    }

    public String requireUniqueOwnership(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("门店不能为空");
        }
        Store currentStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("门店不存在"));
        String hotelId = SuHotelIdUtil.normalize(currentStore.getSuHotelId());
        if (hotelId == null) {
            throw new IllegalStateException("门店尚未配置 Su hotel_id，不能执行评价渠道操作");
        }
        Store uniqueOwner = resolveUniqueOwner(hotelId)
                .orElseThrow(() -> new IllegalStateException(
                        "Su hotel_id 未唯一归属当前门店，已拒绝评价渠道操作"
                ));
        if (!storeId.equals(uniqueOwner.getId())) {
            throw new IllegalStateException("Su hotel_id 不属于当前门店，已拒绝评价渠道操作");
        }
        return hotelId;
    }
}
