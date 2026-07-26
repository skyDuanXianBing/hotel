package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import server.demo.config.SuApiConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuApiClientReviewEndpointTest {

    private RestTemplate restTemplate;
    private SuApiClient client;

    @BeforeEach
    void setUp() {
        SuApiConfig config = new SuApiConfig();
        config.setBaseUrl("https://connect-sandbox.su-api.com");
        config.setClientId("client-id");
        config.setClientSecret("client-secret");
        restTemplate = Mockito.mock(RestTemplate.class);
        client = new SuApiClient();
        ReflectionTestUtils.setField(client, "suApiConfig", config);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    @Test
    void usesCurrentReviewListAndReplyEndpoints() {
        String listUrl = "https://connect-sandbox.su-api.com/SUAPI/jservice/review/list";
        String replyUrl = "https://connect-sandbox.su-api.com/SUAPI/jservice/review/reply";
        when(restTemplate.exchange(
                eq(listUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"Status\":\"Success\",\"Data\":{\"reviews\":[]}}"));
        when(restTemplate.exchange(
                eq(replyUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"Status\":\"Success\",\"Message\":\"ok\"}"));

        JsonNode list = client.pullReviews("token", Map.of("hotel_id", "HOTEL1"));
        JsonNode reply = client.submitReviewReply("token", Map.of(
                "hotel_id", "HOTEL1",
                "channel_id", "19",
                "channel_property_id", "P1",
                "channel_review_id", "R1",
                "review_reply", "Thanks"
        ));

        assertTrue(client.isSuSuccess(list));
        assertTrue(client.isSuSuccess(reply));
        verify(restTemplate).exchange(eq(listUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        verify(restTemplate).exchange(eq(replyUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
}
