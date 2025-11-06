package server.demo.dto;

import server.demo.entity.RoomStatusShare;
import java.util.List;

public class RoomStatusShareResponse {
    
    private List<RoomStatusShareDto> shares;
    private int total;
    private int page;
    private int pageSize;

    // Constructors
    public RoomStatusShareResponse() {}

    public RoomStatusShareResponse(List<RoomStatusShareDto> shares, int total, int page, int pageSize) {
        this.shares = shares;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public List<RoomStatusShareDto> getShares() {
        return shares;
    }

    public void setShares(List<RoomStatusShareDto> shares) {
        this.shares = shares;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    // DTO for share item
    public static class RoomStatusShareDto {
        private Long id;
        private String shareTitle;
        private int roomCount;
        private String roomNumbers;
        private String shareLink;
        private String createdAt;
        private String updatedAt;
        private Boolean isActive;

        // Constructors
        public RoomStatusShareDto() {}

        public RoomStatusShareDto(RoomStatusShare share, String roomNumbers, int roomCount) {
            this.id = share.getId();
            this.shareTitle = share.getShareTitle();
            this.roomCount = roomCount;
            this.roomNumbers = roomNumbers;
            this.shareLink = share.getShareLink();
            this.createdAt = share.getCreatedAt() != null ? share.getCreatedAt().toString() : null;
            this.updatedAt = share.getUpdatedAt() != null ? share.getUpdatedAt().toString() : null;
            this.isActive = share.getIsActive();
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getShareTitle() {
            return shareTitle;
        }

        public void setShareTitle(String shareTitle) {
            this.shareTitle = shareTitle;
        }

        public int getRoomCount() {
            return roomCount;
        }

        public void setRoomCount(int roomCount) {
            this.roomCount = roomCount;
        }

        public String getRoomNumbers() {
            return roomNumbers;
        }

        public void setRoomNumbers(String roomNumbers) {
            this.roomNumbers = roomNumbers;
        }

        public String getShareLink() {
            return shareLink;
        }

        public void setShareLink(String shareLink) {
            this.shareLink = shareLink;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }
}