package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RoomStatusShareRequest {
    
    @NotBlank(message = "分享标题不能为空")
    private String shareTitle;

    @NotNull(message = "查看房间状态设置不能为空")
    private Boolean viewRoomStatus;

    @NotNull(message = "查询方式不能为空")
    private Boolean queryMethod;

    @NotBlank(message = "查看类型不能为空")
    private String viewType;

    @NotBlank(message = "查询模式不能为空")
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