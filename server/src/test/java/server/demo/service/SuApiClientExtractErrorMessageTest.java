package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuApiClientExtractErrorMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SuApiClient suApiClient = new SuApiClient();

    @Test
    void extractSuErrorMessage_readsResponseMessage() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "status": "Error",
                  "response": {
                    "code": "400",
                    "message": "This account doesn't have access to this property."
                  }
                }
                """);

        assertEquals(
                "This account doesn't have access to this property.",
                suApiClient.extractSuErrorMessage(response)
        );
        assertEquals("400", suApiClient.extractSuErrorCode(response));
    }

    @Test
    void extractSuErrorMessage_fallsBackToErrorsShortText() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "Errors": [
                    { "Code": "E123", "ShortText": "listingid is required" }
                  ]
                }
                """);

        assertEquals("listingid is required", suApiClient.extractSuErrorMessage(response));
        assertEquals("E123", suApiClient.extractSuErrorCode(response));
    }
}

