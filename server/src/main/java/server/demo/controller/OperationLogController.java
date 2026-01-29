package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.OperationLogDTO;
import server.demo.service.OperationLogService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operation-logs")
@CrossOrigin
@StoreScoped
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ApiResponse<List<OperationLogDTO>>> getReservationLogs(@PathVariable Long reservationId) {
        List<OperationLogDTO> logs = operationLogService.getReservationLogs(reservationId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

