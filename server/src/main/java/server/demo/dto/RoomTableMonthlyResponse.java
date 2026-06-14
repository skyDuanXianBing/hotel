package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomTableMonthlyResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<MonthlyRoomDataDTO> rooms;
    private List<MonthlyRoomTypeSummaryDTO> roomTypeSummaries;

    public RoomTableMonthlyResponse() {}

    public RoomTableMonthlyResponse(
            LocalDate startDate,
            LocalDate endDate,
            List<MonthlyRoomDataDTO> rooms,
            List<MonthlyRoomTypeSummaryDTO> roomTypeSummaries
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.rooms = rooms;
        this.roomTypeSummaries = roomTypeSummaries;
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

    public List<MonthlyRoomDataDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<MonthlyRoomDataDTO> rooms) {
        this.rooms = rooms;
    }

    public List<MonthlyRoomTypeSummaryDTO> getRoomTypeSummaries() {
        return roomTypeSummaries;
    }

    public void setRoomTypeSummaries(List<MonthlyRoomTypeSummaryDTO> roomTypeSummaries) {
        this.roomTypeSummaries = roomTypeSummaries;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonthlyRoomDataDTO {
        private Long roomId;
        private String roomNumber;
        private Long roomTypeId;
        private String roomType;
        private List<MonthlyDailyStatusDTO> dailyStatus;

        public MonthlyRoomDataDTO() {}

        public MonthlyRoomDataDTO(
                Long roomId,
                String roomNumber,
                Long roomTypeId,
                String roomType,
                List<MonthlyDailyStatusDTO> dailyStatus
        ) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.roomTypeId = roomTypeId;
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

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public String getRoomType() {
            return roomType;
        }

        public void setRoomType(String roomType) {
            this.roomType = roomType;
        }

        public List<MonthlyDailyStatusDTO> getDailyStatus() {
            return dailyStatus;
        }

        public void setDailyStatus(List<MonthlyDailyStatusDTO> dailyStatus) {
            this.dailyStatus = dailyStatus;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonthlyDailyStatusDTO {
        private LocalDate date;
        private String status;
        private String displayStatus;
        private Boolean sellable;
        private String blockedReason;
        private DailyRoomStatusDTO.ReservationInfoDTO reservation;
        private Boolean closed;
        private String closeType;
        private String closeRemark;
        private Integer roomTypeAvailableRooms;
        private Boolean closeRoom;
        private Boolean cta;
        private Boolean ctd;

        public MonthlyDailyStatusDTO() {}

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDisplayStatus() {
            return displayStatus;
        }

        public void setDisplayStatus(String displayStatus) {
            this.displayStatus = displayStatus;
        }

        public Boolean getSellable() {
            return sellable;
        }

        public void setSellable(Boolean sellable) {
            this.sellable = sellable;
        }

        public String getBlockedReason() {
            return blockedReason;
        }

        public void setBlockedReason(String blockedReason) {
            this.blockedReason = blockedReason;
        }

        public DailyRoomStatusDTO.ReservationInfoDTO getReservation() {
            return reservation;
        }

        public void setReservation(DailyRoomStatusDTO.ReservationInfoDTO reservation) {
            this.reservation = reservation;
        }

        public Boolean getClosed() {
            return closed;
        }

        public void setClosed(Boolean closed) {
            this.closed = closed;
        }

        public String getCloseType() {
            return closeType;
        }

        public void setCloseType(String closeType) {
            this.closeType = closeType;
        }

        public String getCloseRemark() {
            return closeRemark;
        }

        public void setCloseRemark(String closeRemark) {
            this.closeRemark = closeRemark;
        }

        public Integer getRoomTypeAvailableRooms() {
            return roomTypeAvailableRooms;
        }

        public void setRoomTypeAvailableRooms(Integer roomTypeAvailableRooms) {
            this.roomTypeAvailableRooms = roomTypeAvailableRooms;
        }

        public Boolean getCloseRoom() {
            return closeRoom;
        }

        public void setCloseRoom(Boolean closeRoom) {
            this.closeRoom = closeRoom;
        }

        public Boolean getCta() {
            return cta;
        }

        public void setCta(Boolean cta) {
            this.cta = cta;
        }

        public Boolean getCtd() {
            return ctd;
        }

        public void setCtd(Boolean ctd) {
            this.ctd = ctd;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonthlyRoomTypeSummaryDTO {
        private Long roomTypeId;
        private String roomTypeName;
        private LocalDate date;
        private Integer totalRooms;
        private Integer physicalSellableRooms;
        private Integer assignedOccupiedRooms;
        private Integer blockoutRooms;
        private Integer staticUnavailableRooms;
        private Integer unassignedOccupiedRooms;
        private Integer inventoryLimit;
        private Integer effectiveAvailableRooms;
        private Boolean closeRoom;
        private Boolean cta;
        private Boolean ctd;

        public MonthlyRoomTypeSummaryDTO() {}

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public String getRoomTypeName() {
            return roomTypeName;
        }

        public void setRoomTypeName(String roomTypeName) {
            this.roomTypeName = roomTypeName;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Integer getTotalRooms() {
            return totalRooms;
        }

        public void setTotalRooms(Integer totalRooms) {
            this.totalRooms = totalRooms;
        }

        public Integer getPhysicalSellableRooms() {
            return physicalSellableRooms;
        }

        public void setPhysicalSellableRooms(Integer physicalSellableRooms) {
            this.physicalSellableRooms = physicalSellableRooms;
        }

        public Integer getAssignedOccupiedRooms() {
            return assignedOccupiedRooms;
        }

        public void setAssignedOccupiedRooms(Integer assignedOccupiedRooms) {
            this.assignedOccupiedRooms = assignedOccupiedRooms;
        }

        public Integer getBlockoutRooms() {
            return blockoutRooms;
        }

        public void setBlockoutRooms(Integer blockoutRooms) {
            this.blockoutRooms = blockoutRooms;
        }

        public Integer getStaticUnavailableRooms() {
            return staticUnavailableRooms;
        }

        public void setStaticUnavailableRooms(Integer staticUnavailableRooms) {
            this.staticUnavailableRooms = staticUnavailableRooms;
        }

        public Integer getUnassignedOccupiedRooms() {
            return unassignedOccupiedRooms;
        }

        public void setUnassignedOccupiedRooms(Integer unassignedOccupiedRooms) {
            this.unassignedOccupiedRooms = unassignedOccupiedRooms;
        }

        public Integer getInventoryLimit() {
            return inventoryLimit;
        }

        public void setInventoryLimit(Integer inventoryLimit) {
            this.inventoryLimit = inventoryLimit;
        }

        public Integer getEffectiveAvailableRooms() {
            return effectiveAvailableRooms;
        }

        public void setEffectiveAvailableRooms(Integer effectiveAvailableRooms) {
            this.effectiveAvailableRooms = effectiveAvailableRooms;
        }

        public Boolean getCloseRoom() {
            return closeRoom;
        }

        public void setCloseRoom(Boolean closeRoom) {
            this.closeRoom = closeRoom;
        }

        public Boolean getCta() {
            return cta;
        }

        public void setCta(Boolean cta) {
            this.cta = cta;
        }

        public Boolean getCtd() {
            return ctd;
        }

        public void setCtd(Boolean ctd) {
            this.ctd = ctd;
        }
    }
}
