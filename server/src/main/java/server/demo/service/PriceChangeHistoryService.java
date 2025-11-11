package server.demo.service;

import server.demo.dto.PriceChangeHistoryDTO;
import server.demo.dto.PriceChangeHistoryPageResponse;

import java.time.LocalDate;

/**
 * 改价历史服务接口
 */
public interface PriceChangeHistoryService {

    /**
     * 分页查询改价历史
     *
     * @param operateDateStart 操作日期开始
     * @param operateDateEnd 操作日期结束
     * @param priceDateStart 价格日期开始
     * @param priceDateEnd 价格日期结束
     * @param roomTypeId 房型ID
     * @param pricePlanId 价格计划ID
     * @param operator 操作人
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PriceChangeHistoryPageResponse getPriceChangeHistory(
            LocalDate operateDateStart,
            LocalDate operateDateEnd,
            LocalDate priceDateStart,
            LocalDate priceDateEnd,
            Long roomTypeId,
            Long pricePlanId,
            String operator,
            Integer pageNum,
            Integer pageSize
    );

    /**
     * 创建改价历史记录
     *
     * @param roomTypeId 房型ID
     * @param pricePlanId 价格计划ID
     * @param priceDateStart 价格日期开始
     * @param priceDateEnd 价格日期结束
     * @param applyWeekdays 适用星期
     * @param changeValue 修改后的值
     * @param previousValue 修改前的值
     * @param operator 操作人
     * @param userId 用户ID
     */
    void createPriceChangeHistory(
            Long roomTypeId,
            Long pricePlanId,
            LocalDate priceDateStart,
            LocalDate priceDateEnd,
            String applyWeekdays,
            java.math.BigDecimal changeValue,
            java.math.BigDecimal previousValue,
            String operator,
            Long userId
    );
}
