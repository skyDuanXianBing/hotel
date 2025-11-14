package server.demo.service;

import server.demo.dto.BulkPriceChangeRequest;
import server.demo.dto.RoomPriceDTO;
import server.demo.dto.RoomPriceManagementDTO;
import server.demo.dto.UpdateRoomPriceRequest;
import server.demo.dto.UpdatePriceByPlanRequest;
import server.demo.entity.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface RoomPriceService {

    /**
     * 获取指定日期范围内所有房型的价格数据
     */
    List<RoomPriceDTO> getRoomPricesByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定房型和日期范围内的价格数据
     */
    List<RoomPriceDTO> getRoomPricesByRoomTypeAndDateRange(Long roomTypeId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定房型列表和日期范围内的价格数据
     */
    List<RoomPriceDTO> getRoomPricesByRoomTypesAndDateRange(List<Long> roomTypeIds, LocalDate startDate, LocalDate endDate);

    /**
     * 获取特定房型和日期的价格
     */
    Optional<RoomPriceDTO> getRoomPrice(Long roomTypeId, LocalDate date);

    /**
     * 获取房型在指定日期的实际价格（含默认价格逻辑）
     */
    BigDecimal getEffectivePrice(Long roomTypeId, LocalDate date);

    /**
     * 获取房型在指定日期的实际价格（含默认价格逻辑）
     */
    BigDecimal getEffectivePrice(RoomType roomType, LocalDate date);

    /**
     * 更新房型价格（支持日期范围）
     */
    List<RoomPriceDTO> updateRoomPrice(UpdateRoomPriceRequest request);

    /**
     * 删除指定房型和日期的价格设置
     */
    boolean deleteRoomPrice(Long roomTypeId, LocalDate date);

    /**
     * 删除指定房型和日期范围的价格设置
     */
    int deleteRoomPriceRange(Long roomTypeId, LocalDate startDate, LocalDate endDate);

    /**
     * 批量创建或更新价格
     */
    List<RoomPriceDTO> batchUpdateRoomPrices(List<UpdateRoomPriceRequest> requests);

    /**
     * 批量改价（支持多房型、多日期范围、星期筛选、平日周末价）
     */
    List<RoomPriceDTO> bulkPriceChange(BulkPriceChangeRequest request);

    /**
     * 获取指定日期范围内的房价管理数据(包含价格计划)
     */
    List<RoomPriceManagementDTO> getRoomPriceManagementData(LocalDate startDate, LocalDate endDate, Long roomTypeId);

    /**
     * 按价格计划更新价格
     */
    List<RoomPriceManagementDTO> updatePriceByPlan(UpdatePriceByPlanRequest request, String operator);

    /**
     * 获取指定房型、价格计划和日期的价格
     */
    Optional<RoomPriceManagementDTO> getRoomPriceByPlan(Long roomTypeId, Long pricePlanId, LocalDate date);
}