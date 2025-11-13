package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.PriceChangeHistoryDTO;
import server.demo.dto.PriceChangeHistoryPageResponse;
import server.demo.entity.PriceChangeHistory;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.User;
import server.demo.repository.PriceChangeHistoryRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PriceChangeHistoryServiceImpl implements PriceChangeHistoryService {

    @Autowired
    private PriceChangeHistoryRepository priceChangeHistoryRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private PricePlanRepository pricePlanRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public PriceChangeHistoryPageResponse getPriceChangeHistory(
            Long userId,
            LocalDate operateDateStart,
            LocalDate operateDateEnd,
            LocalDate priceDateStart,
            LocalDate priceDateEnd,
            Long roomTypeId,
            Long pricePlanId,
            String operator,
            Integer pageNum,
            Integer pageSize
    ) {
        // 转换日期为时间范围
        LocalDateTime operateStartTime = operateDateStart != null ? operateDateStart.atStartOfDay() : null;
        LocalDateTime operateEndTime = operateDateEnd != null ? operateDateEnd.atTime(23, 59, 59) : null;

        // 创建分页对象
        Pageable pageable = PageRequest.of(
                pageNum != null && pageNum > 0 ? pageNum - 1 : 0,
                pageSize != null && pageSize > 0 ? pageSize : 25
        );

        // 查询数据 - 添加userId过滤
        Page<PriceChangeHistory> page = priceChangeHistoryRepository.findByConditionsAndUserId(
                userId,
                roomTypeId,
                pricePlanId,
                operator,
                operateStartTime,
                operateEndTime,
                priceDateStart,
                priceDateEnd,
                pageable
        );

        // 转换为DTO
        List<PriceChangeHistoryDTO> records = page.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PriceChangeHistoryPageResponse(
                page.getTotalElements(),
                records,
                pageNum,
                pageSize
        );
    }

    @Override
    public void createPriceChangeHistory(
            Long roomTypeId,
            Long pricePlanId,
            LocalDate priceDateStart,
            LocalDate priceDateEnd,
            String applyWeekdays,
            BigDecimal changeValue,
            BigDecimal previousValue,
            String operator,
            Long userId
    ) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("房型不存在"));

        PricePlan pricePlan = null;
        if (pricePlanId != null) {
            pricePlan = pricePlanRepository.findById(pricePlanId)
                    .orElseThrow(() -> new IllegalArgumentException("价格计划不存在"));
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        PriceChangeHistory history = new PriceChangeHistory();
        history.setRoomType(roomType);
        history.setPricePlan(pricePlan);
        history.setPriceDateStart(priceDateStart);
        history.setPriceDateEnd(priceDateEnd);
        history.setApplyWeekdays(applyWeekdays != null ? applyWeekdays : "全部");
        history.setChangeType("价格");
        history.setChangeValue(changeValue);
        history.setPreviousValue(previousValue);
        history.setOperator(operator);
        history.setUser(user);
        history.setOperateTime(LocalDateTime.now());

        priceChangeHistoryRepository.save(history);
    }

    /**
     * 将实体转换为DTO
     */
    private PriceChangeHistoryDTO convertToDTO(PriceChangeHistory history) {
        PriceChangeHistoryDTO dto = new PriceChangeHistoryDTO();
        dto.setId(history.getId());
        dto.setRoomTypeName(history.getRoomType().getName());
        dto.setPricePlanName(history.getPricePlan() != null ? history.getPricePlan().getName() : "");

        // 格式化日期范围
        String priceDate = DATE_FORMATTER.format(history.getPriceDateStart()) + "至" +
                          DATE_FORMATTER.format(history.getPriceDateEnd());
        dto.setPriceDate(priceDate);

        dto.setApplyDays(history.getApplyWeekdays() != null ? history.getApplyWeekdays() : "全部");
        dto.setChangeType(history.getChangeType());
        dto.setChangeValue(history.getChangeValue());
        dto.setPreviousValue(history.getPreviousValue());
        dto.setOperator(history.getOperator());
        dto.setOperateTime(DATETIME_FORMATTER.format(history.getOperateTime()));

        if (history.getPmsPushTime() != null) {
            dto.setPmsPushTime(DATETIME_FORMATTER.format(history.getPmsPushTime()));
        }

        dto.setNotes(history.getNotes());

        return dto;
    }
}
