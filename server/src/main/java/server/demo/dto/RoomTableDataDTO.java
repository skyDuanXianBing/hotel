package server.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 房情表数据DTO
 */
public class RoomTableDataDTO {
    private LocalDate date; // 统计日期
    private List<RoomTableStatisticsDTO> statistics; // 各房型统计
    private RoomTableStatisticsDTO total; // 合计数据

    public RoomTableDataDTO() {}

    public RoomTableDataDTO(LocalDate date, List<RoomTableStatisticsDTO> statistics, RoomTableStatisticsDTO total) {
        this.date = date;
        this.statistics = statistics;
        this.total = total;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<RoomTableStatisticsDTO> getStatistics() {
        return statistics;
    }

    public void setStatistics(List<RoomTableStatisticsDTO> statistics) {
        this.statistics = statistics;
    }

    public RoomTableStatisticsDTO getTotal() {
        return total;
    }

    public void setTotal(RoomTableStatisticsDTO total) {
        this.total = total;
    }
}