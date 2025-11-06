package server.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 远期房情表响应DTO
 */
public class FutureRoomTableResponse {
    private String startDate;
    private String endDate;
    private List<FutureRoomTypeData> roomTypes;
    private FutureRoomTypeData total;
    private List<FutureRoomStatistics> statistics;

    // 构造函数
    public FutureRoomTableResponse() {}

    public FutureRoomTableResponse(String startDate, String endDate, 
                                 List<FutureRoomTypeData> roomTypes, 
                                 FutureRoomTypeData total,
                                 List<FutureRoomStatistics> statistics) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.roomTypes = roomTypes;
        this.total = total;
        this.statistics = statistics;
    }

    // Getters and Setters
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<FutureRoomTypeData> getRoomTypes() {
        return roomTypes;
    }

    public void setRoomTypes(List<FutureRoomTypeData> roomTypes) {
        this.roomTypes = roomTypes;
    }

    public FutureRoomTypeData getTotal() {
        return total;
    }

    public void setTotal(FutureRoomTypeData total) {
        this.total = total;
    }

    public List<FutureRoomStatistics> getStatistics() {
        return statistics;
    }

    public void setStatistics(List<FutureRoomStatistics> statistics) {
        this.statistics = statistics;
    }
}