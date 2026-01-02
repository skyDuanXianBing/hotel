package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuReservationNotifPayloadParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractNotifIds_empty_returnsEmpty() throws Exception {
        JsonNode root = objectMapper.readTree("{}");
        assertEquals(List.of(), SuReservationNotifPayloadParser.extractNotifIds(root));
    }

    @Test
    void extractNotifIds_dedupAndTrim() throws Exception {
        String json = """
                {
                  "reservation_notif": {
                    "reservation_notif_id": [" 1 ", "2", "1", "", null]
                  }
                }
                """;
        JsonNode root = objectMapper.readTree(json);
        assertEquals(List.of("1", "2"), SuReservationNotifPayloadParser.extractNotifIds(root));
    }
}

