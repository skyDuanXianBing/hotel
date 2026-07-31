package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RoomStatusShareRequest {
    
    @NotBlank(message = "{api.t.922e72ab74ac}")
    private String shareTitle;

    @NotNull(message = "{api.t.c957524597fa}")
    private Boolean viewRoomStatus;

    @NotNull(message = "{api.t.357c9d86b7c1}")
    private Boolean queryMethod;

    @NotBlank(message = "{api.t.22ed8b8d3d6d}")
    private String viewType;

    @NotBlank(message = "{api.t.df33384b8031}")
    private String queryMode;

    private List<String> filterItems;
    
    private List<String> orderItems;
    
    private List<Long> associatedRooms;

    // Constructors
    public RoomStatusShareRequest() {}

    // Getters and Setters
    public String getShareTitle() {
        return shareTitle;
    }

    public void setShareTitle(String shareTitle) {
        this.shareTitle = shareTitle;
    }

    public Boolean getViewRoomStatus() {
        return viewRoomStatus;
    }

    public void setViewRoomStatus(Boolean viewRoomStatus) {
        this.viewRoomStatus = viewRoomStatus;
    }

    public Boolean getQueryMethod() {
        return queryMethod;
    }

    public void setQueryMethod(Boolean queryMethod) {
        this.queryMethod = queryMethod;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(String queryMode) {
        this.queryMode = queryMode;
    }

    public List<String> getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(List<String> filterItems) {
        this.filterItems = filterItems;
    }

    public List<String> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<String> orderItems) {
        this.orderItems = orderItems;
    }

    public List<Long> getAssociatedRooms() {
        return associatedRooms;
    }

    public void setAssociatedRooms(List<Long> associatedRooms) {
        this.associatedRooms = associatedRooms;
    }
}