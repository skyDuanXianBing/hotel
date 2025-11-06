package server.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.BulkPriceChangeRequest;
import server.demo.dto.RoomPriceDTO;
import server.demo.dto.UpdateRoomPriceRequest;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.service.RoomPriceService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomPriceServiceImpl implements RoomPriceService {

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Override
    public List<RoomPriceDTO> getRoomPricesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<RoomPrice> roomPrices = roomPriceRepository.findByDateRangeWithRoomType(startDate, endDate);
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomPriceDTO> getRoomPricesByRoomTypeAndDateRange(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        List<RoomPrice> roomPrices = roomPriceRepository.findByRoomTypeIdAndDateRange(roomTypeId, startDate, endDate);
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomPriceDTO> getRoomPricesByRoomTypesAndDateRange(List<Long> roomTypeIds, LocalDate startDate, LocalDate endDate) {
        List<RoomPrice> roomPrices = roomPriceRepository.findByRoomTypeIdsAndDateRange(roomTypeIds, startDate, endDate);
        return roomPrices.stream()
                .map(RoomPriceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RoomPriceDTO> getRoomPrice(Long roomTypeId, LocalDate date) {
        Optional<RoomPrice> roomPrice = roomPriceRepository.findByRoomTypeIdAndPriceDate(roomTypeId, date);
        return roomPrice.map(RoomPriceDTO::new);
    }

    @Override
    public BigDecimal getEffectivePrice(Long roomTypeId, LocalDate date) {
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findById(roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return getEffectivePrice(roomTypeOpt.get(), date);
    }

    @Override
    public BigDecimal getEffectivePrice(RoomType roomType, LocalDate date) {
        // 1. 首先查找是否有特定日期的价格设置
        Optional<RoomPrice> specificPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomType, date);
        if (specificPrice.isPresent()) {
            return specificPrice.get().getPrice();
        }

        // 2. 如果没有特定日期价格，则使用房型的默认价格逻辑
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        boolean isWeekend = dayOfWeek >= 6; // Saturday=6, Sunday=7

        if (isWeekend && roomType.getWeekendPrice() != null) {
            return roomType.getWeekendPrice();
        } else if (!isWeekend && roomType.getWeekdayPrice() != null) {
            return roomType.getWeekdayPrice();
        } else if (roomType.getDefaultPrice() != null) {
            return roomType.getDefaultPrice();
        }

        // 3. 如果都没有设置，返回0
        return BigDecimal.ZERO;
    }

    @Override
    public List<RoomPriceDTO> updateRoomPrice(UpdateRoomPriceRequest request) {
        // 验证房型存在
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findById(request.getRoomTypeId());
        if (roomTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("房型不存在: " + request.getRoomTypeId());
        }

        RoomType roomType = roomTypeOpt.get();
        List<RoomPriceDTO> updatedPrices = new ArrayList<>();

        // 遍历日期范围，为每一天创建或更新价格
        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            // 查找是否已存在该日期的价格
            Optional<RoomPrice> existingPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomType, currentDate);

            RoomPrice roomPrice;
            if (existingPrice.isPresent()) {
                // 更新现有价格
                roomPrice = existingPrice.get();
                roomPrice.setPrice(request.getPrice());
            } else {
                // 创建新价格记录
                roomPrice = new RoomPrice(roomType, currentDate, request.getPrice());
            }

            // 设置假期标记
            if (request.getIsHoliday() != null) {
                roomPrice.setIsHoliday(request.getIsHoliday());
            }

            // 设置备注
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                roomPrice.setNotes(request.getNotes());
            }

            // 保存价格
            roomPrice = roomPriceRepository.save(roomPrice);
            updatedPrices.add(new RoomPriceDTO(roomPrice));

            // 移动到下一天
            currentDate = currentDate.plusDays(1);
        }

        return updatedPrices;
    }

    @Override
    public boolean deleteRoomPrice(Long roomTypeId, LocalDate date) {
        Optional<RoomType> roomTypeOpt = roomTypeRepository.findById(roomTypeId);
        if (roomTypeOpt.isEmpty()) {
            return false;
        }

        Optional<RoomPrice> roomPrice = roomPriceRepository.findByRoomTypeAndPriceDate(roomTypeOpt.get(), date);
        if (roomPrice.isPresent()) {
            roomPriceRepository.delete(roomPrice.get());
            return true;
        }
        return false;
    }

    @Override
    public int deleteRoomPriceRange(Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        List<RoomPrice> pricesToDelete = roomPriceRepository.findByRoomTypeIdAndDateRange(roomTypeId, startDate, endDate);
        int count = pricesToDelete.size();
        roomPriceRepository.deleteAll(pricesToDelete);
        return count;
    }

    @Override
    public List<RoomPriceDTO> batchUpdateRoomPrices(List<UpdateRoomPriceRequest> requests) {
        List<RoomPriceDTO> allUpdatedPrices = new ArrayList<>();

        for (UpdateRoomPriceRequest request : requests) {
            try {
                List<RoomPriceDTO> updatedPrices = updateRoomPrice(request);
                allUpdatedPrices.addAll(updatedPrices);
            } catch (Exception e) {
                // 记录错误但继续处理其他请求
                System.err.println("Failed to update room price for roomTypeId " +
                    request.getRoomTypeId() + ": " + e.getMessage());
            }
        }

        return allUpdatedPrices;
    }

    @Override
    public List<RoomPriceDTO> bulkPriceChange(BulkPriceChangeRequest request) {
        // 验证请求数据
        request.validate();

        List<RoomPriceDTO> updatedPrices = new ArrayList<>();

        // 获取所有要更新的房型
        List<RoomType> roomTypes = roomTypeRepository.findAllById(request.getRoomTypeIds());
        if (roomTypes.isEmpty()) {
            throw new IllegalArgumentException("未找到指定的房型");
        }

        // 遍历每个房型
        for (RoomType roomType : roomTypes) {
            // 遍历每个日期范围
            for (BulkPriceChangeRequest.DateRangeDTO dateRange : request.getDateRanges()) {
                LocalDate currentDate = dateRange.getStartDate();

                // 遍历日期范围内的每一天
                while (!currentDate.isAfter(dateRange.getEndDate())) {
                    // 检查该日期是否符合星期几筛选条件
                    if (shouldApplyPriceForDate(currentDate, request.getWeekdays())) {
                        // 确定该日期使用的价格（平日价或周末价）
                        BigDecimal priceToApply = determinePriceForDate(
                            currentDate,
                            request.getWeekendDifferentiation(),
                            request.getWeekdayPrice(),
                            request.getWeekendPrice()
                        );

                        // 查找是否已存在该日期的价格
                        Optional<RoomPrice> existingPrice = roomPriceRepository
                            .findByRoomTypeAndPriceDate(roomType, currentDate);

                        RoomPrice roomPrice;
                        if (existingPrice.isPresent()) {
                            // 更新现有价格
                            roomPrice = existingPrice.get();
                            roomPrice.setPrice(priceToApply);
                        } else {
                            // 创建新价格记录
                            roomPrice = new RoomPrice(roomType, currentDate, priceToApply);
                        }

                        // 设置备注
                        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                            roomPrice.setNotes(request.getNotes());
                        }

                        // 保存价格
                        roomPrice = roomPriceRepository.save(roomPrice);
                        updatedPrices.add(new RoomPriceDTO(roomPrice));
                    }

                    // 移动到下一天
                    currentDate = currentDate.plusDays(1);
                }
            }
        }

        return updatedPrices;
    }

    /**
     * 判断某个日期是否符合星期几筛选条件
     * @param date 要检查的日期
     * @param weekdays 允许的星期几集合（1=周一, ..., 6=周六, 0=周日）
     * @return 如果weekdays为空或包含该日期的星期几，返回true
     */
    private boolean shouldApplyPriceForDate(LocalDate date, Set<Integer> weekdays) {
        // 如果没有指定星期几筛选，则所有日期都适用
        if (weekdays == null || weekdays.isEmpty() || weekdays.size() == 7) {
            return true;
        }

        // 获取日期的星期几（1=Monday, 7=Sunday）
        int dayOfWeek = date.getDayOfWeek().getValue();

        // 转换为前端的格式（1=周一, ..., 6=周六, 0=周日）
        int weekdayValue = (dayOfWeek == 7) ? 0 : dayOfWeek;

        return weekdays.contains(weekdayValue);
    }

    /**
     * 根据日期和设置确定使用的价格
     * @param date 日期
     * @param weekendDifferentiation 是否区分平日周末
     * @param weekdayPrice 平日价
     * @param weekendPrice 周末价
     * @return 该日期应使用的价格
     */
    private BigDecimal determinePriceForDate(
        LocalDate date,
        Boolean weekendDifferentiation,
        BigDecimal weekdayPrice,
        BigDecimal weekendPrice
    ) {
        // 如果不区分平日周末，直接使用平日价（即统一价格）
        if (!weekendDifferentiation) {
            return weekdayPrice;
        }

        // 判断是否为周末（周五和周六）
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY);

        return isWeekend ? weekendPrice : weekdayPrice;
    }
}