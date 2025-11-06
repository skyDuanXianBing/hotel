package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.PaymentDTO;
import server.demo.entity.Payment;
import server.demo.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 创建收款记录
     */
    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        Payment payment = new Payment();
        payment.setReservationId(dto.getReservationId());
        payment.setType(dto.getType());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setAmount(dto.getAmount());
        payment.setDate(LocalDate.parse(dto.getDate(), DATE_FORMATTER));
        payment.setRemark(dto.getRemark());
        payment.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "系统");

        payment = paymentRepository.save(payment);
        return toDTO(payment);
    }

    /**
     * 根据预订ID获取收款记录列表
     */
    public List<PaymentDTO> getPaymentsByReservationId(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationIdOrderByDateDesc(reservationId);
        return payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除收款记录
     */
    @Transactional
    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
    }

    /**
     * 根据预订ID计算总收款金额
     */
    public BigDecimal getTotalPaymentByReservationId(Long reservationId) {
        List<Payment> payments = paymentRepository.findByReservationIdOrderByDateDesc(reservationId);
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 转换为DTO
     */
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