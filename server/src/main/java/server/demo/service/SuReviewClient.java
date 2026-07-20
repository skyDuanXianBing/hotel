package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SuReviewClient {

    private final SuApiClient suApiClient;
    private final SuAccessTokenService accessTokenService;

    public SuReviewClient(SuApiClient suApiClient, SuAccessTokenService accessTokenService) {
        this.suApiClient = suApiClient;
        this.accessTokenService = accessTokenService;
    }

    public JsonNode pullReviews(Map<String, Object> payload) {
        return accessTokenService.executeWithTokenRetry(
                token -> suApiClient.pullReviews(token, payload),
                "ReviewPull"
        );
    }

    public JsonNode submitReply(Map<String, Object> payload) {
        return accessTokenService.executeWithTokenRetry(
                token -> suApiClient.submitReviewReply(token, payload),
                "ReviewReply"
        );
    }
}
