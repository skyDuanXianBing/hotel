package server.demo.dto;

import java.time.LocalDate;
import java.util.List;

public class RoomStatusCalendarDTO {
    private DateRangeDTO dateRange;
    private List<CalendarRoomDataDTO> rooms;

    public RoomStatusCalendarDTO() {}

    public RoomStatusCalendarDTO(DateRangeDTO dateRange, List<CalendarRoomDataDTO> rooms) {
        this.dateRange = dateRange;
        this.rooms = rooms;
    }

    public DateRangeDTO getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeDTO dateRange) {
        this.dateRange = dateRange;
    }

    public List<CalendarRoomDataDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<CalendarRoomDataDTO> rooms) {
        this.rooms = rooms;
    }

    public static class DateRangeDTO {
        private LocalDate startDate;
        private LocalDate endDate;

        public DateRangeDTO() {}

        public DateRangeDTO(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

    public static class CalendarRoomDataDTO {
        private Long roomId;
        private String roomNumber;
        private String roomType;
        private List<DailyRoomStatusDTO> dailyStatus;

        public CalendarRoomDataDTO() {}

        public CalendarRoomDataDTO(Long roomId, String roomNumber, String roomType, List<DailyRoomStatusDTO> dailyStatus) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.dailyStatus = dailyStatus;
        }

        public Long getRoomId() {
            return roomId;
        }

        public void setRoomId(Long roomId) {
            this.roomId = roomId;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        public String getRoomType() {
            return roomType;
        }

        public void setRoomType(String roomType) {
            this.roomType = roomType;
        }

        public List<DailyRoomStatusDTO> getDailyStatus() {
            return dailyStatus;
        }

        public void setDailyStatus(List<DailyRoomStatusDTO> dailyStatus) {
            this.dailyStatus = dailyStatus;
        }
    }
}