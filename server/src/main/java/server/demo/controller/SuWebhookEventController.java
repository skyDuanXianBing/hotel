package server.demo.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.repository.SuReservationWebhookEventRepository;
import server.demo.service.SuReservationWebhookCompensationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/su/webhook-events")
public class SuWebhookEventController {

    private final SuReservationWebhookEventRepository eventRepository;
    private final SuReservationWebhookCompensationService compensationService;

    public SuWebhookEventController(
            SuReservationWebhookEventRepository eventRepository,
            SuReservationWebhookCompensationService compensationService
    ) {
        this.eventRepository = eventRepository;
        this.compensationService = compensationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SuReservationWebhookEvent>>> list(
            @RequestParam(defaultValue = "FAILED") SuWebhookEventStatus status,
            @RequestParam(defaultValue = "50") int size
    ) {
        int pageSize = Math.max(1, Math.min(size, 200));
        List<SuReservationWebhookEvent> items = eventRepository.findByStatusOrderByUpdatedAtDesc(status, PageRequest.of(0, pageSize));
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", items));
    }

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<Integer>> processNow(@RequestParam(defaultValue = "50") int limit) {
        int processed = compensationService.processDueEventsOnce(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", processed));
    }
}

