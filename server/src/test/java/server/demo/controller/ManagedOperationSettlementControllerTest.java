package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import server.demo.service.ManagedOperationPdfService;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedOperationSettlementControllerTest {

    @Test
    void exportResponse_shouldExposeContentDispositionToCrossOriginBrowser() {
        byte[] bytes = "pdf".getBytes(StandardCharsets.UTF_8);
        ManagedOperationPdfService.ExportFile file = new ManagedOperationPdfService.ExportFile(
                bytes, "application/pdf", "物业-精算書.pdf");

        ResponseEntity<byte[]> response = ManagedOperationSettlementController.buildExportResponse(file);

        assertEquals(HttpHeaders.CONTENT_DISPOSITION,
                response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertArrayEquals(bytes, response.getBody());
    }
}
