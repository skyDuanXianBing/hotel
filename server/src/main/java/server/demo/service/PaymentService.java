package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.OperationLogDetailDTO;
import server.demo.dto.PaymentDTO;
import server.demo.entity.Payment;
import server.demo.entity.Reservation;
import server.demo.enums.OperationType;
import server.demo.repository.PaymentRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SYSTEM_OPERATOR = "\u7cfb\u7edf";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OperationLogService operationLogService;

    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        loadReservationInStore(dto.getReservationId());

        Payment payment = new Payment();
        payment.setReservationId(dto.getReservationId());
        payment.setType(dto.getType());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setAmount(dto.getAmount());
        payment.setDate(LocalDate.parse(dto.getDate(), DATE_FORMATTER));
        payment.setRemark(dto.getRemark());
        payment.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : SYSTEM_OPERATOR);

        payment = paymentRepository.save(payment);

        operationLogService.logOperation(
                dto.getReservationId(),
                OperationType.BILLING,
                resolvePaymentAction(dto.getType()),
                null,
                null,
                List.of(
                        new OperationLogDetailDTO("\u7c7b\u578b", dto.getType()),
                        new OperationLogDetailDTO("\u91d1\u989d", dto.getAmount() == null ? "0.00" : dto.getAmount().toPlainString()),
                        new OperationLogDetailDTO("\u65e5\u671f", dto.getDate()),
                        new OperationLogDetailDTO("\u652f\u4ed8\u65b9\u5f0f", dto.getPaymentMethod()),
                        new OperationLogDetailDTO("\u5907\u6ce8", dto.getRemark() == null ? "-" : dto.getRemark())
                )
        );

        return toDTO(payment);
    }

    public List<PaymentDTO> getPaymentsByReservationId(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationIdOrderByDateDesc(reservationId);
        return payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }

    public BigDecimal getTotalPaymentByReservationId(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationIdOrderByDateDesc(reservationId);
        return payments.stream()
                .map(Payment::getAmount)
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

    private String resolvePaymentAction(String type) {
        if (type == null) {
            return "\u6536\u6b3e";
        }
        String normalized = type.trim().toLowerCase();
        return switch (normalized) {
            case "refund" -> "\u9000\u6b3e";
            case "deposit" -> "\u6536\u62bc\u91d1";
            default -> "\u6536\u6b3e";
        };
    }

    private PaymentDTO toDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setReservationId(payment.getReservationId());
        dto.setType(payment.getType());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setDate(payment.getDate().format(DATE_FORMATTER));
        dto.setRemark(payment.getRemark());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setCreatedAt(payment.getCreatedAt().toString());
        return dto;
    }
}

