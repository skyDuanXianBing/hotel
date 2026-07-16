package server.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedOperationDtosTest {

    @Test
    void excludedPreviewLine_shouldSerializeCalculatedAmountsAsExplicitNulls() throws Exception {
        ManagedOperationDtos.PreviewLine line = new ManagedOperationDtos.PreviewLine(
                "BOOKING", 2, "123456", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2),
                "Guest", "", "JPY", new BigDecimal("10000"), new BigDecimal("1000"),
                new BigDecimal("100"), new BigDecimal("7273"), null, null, null,
                null, "", ManagedOperationDtos.LineStatus.UNMATCHED, List.of("未匹配"));

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(line);

        assertTrue(json.has("receivedAmount") && json.get("receivedAmount").isNull());
        assertTrue(json.has("managementFee") && json.get("managementFee").isNull());
        assertTrue(json.has("scheduledTransfer") && json.get("scheduledTransfer").isNull());
        assertTrue(json.has("payoutDate") && json.get("payoutDate").isNull());
    }
}
