package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.MoveToOrderBoxRequest;
import server.demo.dto.MoveOutOrderBoxRequest;
import server.demo.dto.OrderBoxDTO;
import server.demo.dto.ReservationDTO;
import server.demo.entity.OrderBox;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.OrderBoxRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderBoxService {

    @Autowired
    private OrderBoxRepository orderBoxRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * 获取订单盒子列表
     */
    public List<OrderBoxDTO> getOrderBoxList() {
        List<OrderBox> orderBoxes = orderBoxRepository.findAllWithDetails();
        return orderBoxes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 移入订单盒子
     */
    public OrderBoxDTO moveToOrderBox(MoveToOrderBoxRequest request) {
        Long reservationId = request.getReservationId();

        // 检查订单是否存在
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查订单状态
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("只有已确认的预订可以移入订单盒子");
        }

        // 检查是否已在订单盒子中
        if (orderBoxRepository.existsByReservationId(reservationId)) {
            throw new RuntimeException("该订单已在订单盒子中");
        }

        // 创建订单盒子记录
        OrderBox orderBox = new OrderBox();
        orderBox.setReservation(reservation);
        orderBox.setMovedInAt(LocalDateTime.now());
        orderBox.setNotes(request.getNotes());

        OrderBox saved = orderBoxRepository.save(orderBox);
        return convertToDTO(saved);
    }

    /**
     * 移出订单盒子
     */
    public void moveOutOrderBox(MoveOutOrderBoxRequest request) {
        Long orderBoxItemId = request.getOrderBoxItemId();

        // 检查订单盒子项是否存在
        OrderBox orderBox = orderBoxRepository.findById(orderBoxItemId)
                .orElseThrow(() -> new RuntimeException("订单盒子记录不存在"));

        // 删除订单盒子记录
        orderBoxRepository.delete(orderBox);
    }

    /**
     * 获取订单盒子统计信息
     */
    public Map<String, Object> getOrderBoxStatistics() {
        long count = orderBoxRepository.countAll();
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("count", count);
        return statistics;
    }

    /**
     * 检查订单是否可以移入订单盒子
     */
    public Map<String, Object> checkCanMoveToOrderBox(Long reservationId) {
        Map<String, Object> result = new HashMap<>();

        // 检查订单是否存在
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElse(null);

        if (reservation == null) {
            result.put("canMove", false);
            result.put("reason", "订单不存在");
            return result;
        }

        // 检查是否已在订单盒子中
        if (orderBoxRepository.existsByReservationId(reservationId)) {
            result.put("canMove", false);
            result.put("reason", "该订单已在订单盒子中");
            return result;
        }

        // 检查订单状态
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            result.put("canMove", false);
            result.put("reason", "只有已确认的预订可以移入订单盒子");
            return result;
        }

        result.put("canMove", true);
        return result;
    }

    /**
     * 转换为DTO
     */
    private OrderBoxDTO convertToDTO(OrderBox orderBox) {
        OrderBoxDTO dto = new OrderBoxDTO();
        dto.setId(orderBox.getId());
        dto.setReservationId(orderBox.getReservation().getId());
        dto.setMovedInAt(orderBox.getMovedInAt());
        dto.setMovedInBy(orderBox.getMovedInBy());
        dto.setNotes(orderBox.getNotes());

        // 转换预订信息
        Reservation reservation = orderBox.getReservation();
        ReservationDTO reservationDTO = new ReservationDTO();
        reservationDTO.setId(reservation.getId());
        reservationDTO.setOrderNumber(reservation.getOrderNumber());
        reservationDTO.setGuestName(reservation.getGuestName());
        reservationDTO.setPhone(reservation.getGuestPhone());
        reservationDTO.setRoomId(reservation.getRoom().getId());
        reservationDTO.setRoomNumber(reservation.getRoom().getRoomNumber());
        reservationDTO.setRoomTypeName(reservation.getRoom().getRoomType().getName());
        reservationDTO.setChannelId(reservation.getChannel().getId());
        reservationDTO.setChannelName(reservation.getChannel().getName());
        reservationDTO.setCheckInDate(reservation.getCheckInDate());
        reservationDTO.setCheckOutDate(reservation.getCheckOutDate());
        reservationDTO.setStatus(reservation.getStatus().name());
        reservationDTO.setNotes(reservation.getNotes());
        reservationDTO.setTotalAmount(reservation.getTotalAmount());
        reservationDTO.setCreatedAt(reservation.getCreatedAt());
        reservationDTO.setUpdatedAt(reservation.getUpdatedAt());

        dto.setReservation(reservationDTO);

        return dto;
    }
}