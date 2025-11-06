package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.ConsumptionDTO;
import server.demo.entity.Consumption;
import server.demo.repository.ConsumptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsumptionService {

    @Autowired
    private ConsumptionRepository consumptionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 创建消费记录
     */
    @Transactional
    public ConsumptionDTO createConsumption(ConsumptionDTO dto) {
        Consumption consumption = new Consumption();
        consumption.setReservationId(dto.getReservationId());
        consumption.setItem(dto.getItem());
        consumption.setQuantity(dto.getQuantity());
        // 确保金额为负数（支出）
        BigDecimal amount = dto.getAmount().abs().negate();
        consumption.setAmount(amount);
        consumption.setDate(LocalDate.parse(dto.getDate(), DATE_FORMATTER));
        consumption.setRemark(dto.getRemark());
        consumption.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "系统");

        consumption = consumptionRepository.save(consumption);
        return toDTO(consumption);
    }

    /**
     * 根据预订ID获取消费记录列表
     */
    public List<ConsumptionDTO> getConsumptionsByReservationId(Long reservationId) {
        List<Consumption> consumptions = consumptionRepository.findByReservationIdOrderByDateDesc(reservationId);
        return consumptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除消费记录
     */
    @Transactional
    public void deleteConsumption(Long id) {
        consumptionRepository.deleteById(id);
    }

    /**
     * 根据预订ID计算总消费金额
     */
    public BigDecimal getTotalConsumptionByReservationId(Long reservationId) {
        List<Consumption> consumptions = consumptionRepository.findByReservationIdOrderByDateDesc(reservationId);
        return consumptions.stream()
                .map(Consumption::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 转换为DTO
     */
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