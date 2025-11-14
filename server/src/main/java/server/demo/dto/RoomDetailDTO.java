package server.demo.dto;

import java.math.BigDecimal;

/**
 * 房间明细DTO
 * 用于房费、间夜、RevPAR等明细表格展示
 */
public class RoomDetailDTO {
    // 房型名称
    private String roomType;

    // 房间号 (可以是具体房间号或"未排房"、"合计")
    private String roomNumber;

    // 合计值
    private BigDecimal total;

    // 当前日期的值
    private BigDecimal currentDate;

    // Constructors
    public RoomDetailDTO() {}

    public RoomDetailDTO(String roomType, String roomNumber, BigDecimal total, BigDecimal currentDate) {
        this.roomType = roomType;
        this.roomNumber = roomNumber;
        this.total = total;
        this.currentDate = currentDate;
    }

    // Getters and Setters
    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(BigDecimal currentDate) {
        this.currentDate = currentDate;
    }
}
