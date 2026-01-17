package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PriceLabsIntegration;
import server.demo.repository.PriceLabsIntegrationRepository;

import java.util.List;

@Service
public class PriceLabsStatusService {

    @Autowired private PriceLabsApiClient apiClient;
    @Autowired private PriceLabsIntegrationRepository integrationRepo;

    @Transactional(readOnly = true)
    public PriceLabsApiClient.PriceLabsResponse queryStatus(Long storeId, List<PriceLabsApiClient.StatusReq> statuses) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId 不能为空");
        }
        PriceLabsIntegration integration = integrationRepo.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + storeId));
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new RuntimeException("PriceLabs 集成未启用");
        }
        return apiClient.queryStatus(statuses);
    }
}

