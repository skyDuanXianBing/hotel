package server.demo.dto;

import java.util.List;

/**
 * 远期房情房型数据DTO
 */
public class FutureRoomTypeData {
    private String roomTypeName;
    private int totalRooms;
    private List<FutureDateRoomData> dates;

    // 构造函数
    public FutureRoomTypeData() {}

    public FutureRoomTypeData(String roomTypeName, int totalRooms, List<FutureDateRoomData> dates) {
        this.roomTypeName = roomTypeName;
        this.totalRooms = totalRooms;
        this.dates = dates;
    }

    // Getters and Setters
    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public List<FutureDateRoomData> getDates() {
        return dates;
    }

    public void setDates(List<FutureDateRoomData> dates) {
        this.dates = dates;
    }
}