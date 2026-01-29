package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.ConsumptionDTO;
import server.demo.dto.OperationLogDetailDTO;
import server.demo.entity.Consumption;
import server.demo.entity.Reservation;
import server.demo.enums.OperationType;
import server.demo.repository.ConsumptionRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ConsumptionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SYSTEM_OPERATOR = "\u7cfb\u7edf";

    @Autowired
    private ConsumptionRepository consumptionRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OperationLogService operationLogService;

    @Transactional
    public ConsumptionDTO createConsumption(ConsumptionDTO dto) {
        loadReservationInStore(dto.getReservationId());

        Consumption consumption = new Consumption();
        consumption.setReservationId(dto.getReservationId());
        consumption.setItem(dto.getItem());
        consumption.setQuantity(dto.getQuantity());

        BigDecimal amount = dto.getAmount().abs().negate();
        consumption.setAmount(amount);
        consumption.setDate(LocalDate.parse(dto.getDate(), DATE_FORMATTER));
        consumption.setRemark(dto.getRemark());
        consumption.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : SYSTEM_OPERATOR);

        consumption = consumptionRepository.save(consumption);

        operationLogService.logOperation(
                dto.getReservationId(),
                OperationType.BILLING,
                "\u6dfb\u52a0\u6d88\u8d39",
                null,
                null,
                List.of(
                        new OperationLogDetailDTO("\u9879\u76ee", dto.getItem()),
                        new OperationLogDetailDTO("\u6570\u91cf", dto.getQuantity() == null ? "1" : String.valueOf(dto.getQuantity())),
                        new OperationLogDetailDTO("\u91d1\u989d", dto.getAmount() == null ? "0.00" : dto.getAmount().abs().toPlainString()),
                        new OperationLogDetailDTO("\u65e5\u671f", dto.getDate()),
                        new OperationLogDetailDTO("\u5907\u6ce8", dto.getRemark() == null ? "-" : dto.getRemark())
                )
        );

        return toDTO(consumption);
    }

    public List<ConsumptionDTO> getConsumptionsByReservationId(Long reservationId) {
        List<Consumption> consumptions = consumptionRepository.findByReservationIdOrderByDateDesc(reservationId);
        return consumptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteConsumption(Long id) {
        consumptionRepository.deleteById(id);
    }

    public BigDecimal getTotalConsumptionByReservationId(Long reservationId) {
        List<Consumption> consumptions = consumptionRepository.findByReservationIdOrderByDateDesc(reservationId);
        return consumptions.stream()
                .map(Consumption::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Reservation loadReservationInStore(Long reservationId) {
        if (reservationId == null) {
            throw new RuntimeException("\u8ba2\u5355ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        Long storeId = StoreContextUtils.requireStoreId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("\u8ba2\u5355\u4e0d\u5b58\u5728"));
        if (!Objects.equals(storeId, reservation.getStoreId())) {
            throw new RuntimeException("\u65e0\u6743\u9650\u64cd\u4f5c\u8be5\u8ba2\u5355");
        }
        return reservation;
    }

    private ConsumptionDTO toDTO(Consumption consumption) {
        ConsumptionDTO dto = new ConsumptionDTO();
        dto.setId(consumption.getId());
        dto.setReservationId(consumption.getReservationId());
        dto.setItem(consumption.getItem());
        dto.setQuantity(consumption.getQuantity());
        dto.setAmount(consumption.getAmount());
        dto.setDate(consumption.getDate().format(DATE_FORMATTER));
        dto.setRemark(consumption.getRemark());
        dto.setCreatedBy(consumption.getCreatedBy());
        dto.setCreatedAt(consumption.getCreatedAt().toString());
        return dto;
    }
}

