package server.demo.dto;

import server.demo.entity.RoomStatusShare;

/**
 * Public room-status share detail response.
 */
public class RoomStatusSharePublicResponse {

    private String shareTitle;
    private Boolean viewRoomStatus;
    private Boolean queryMethod;
    private String viewType;
    private String queryMode;
    private String filterItems;
    private String orderItems;

    public RoomStatusSharePublicResponse() {}

    public RoomStatusSharePublicResponse(RoomStatusShare share) {
        this.shareTitle = share.getShareTitle();
        this.viewRoomStatus = share.getViewRoomStatus();
        this.queryMethod = share.getQueryMethod();
        this.viewType = share.getViewType();
        this.queryMode = share.getQueryMode();
        this.filterItems = share.getFilterItems();
        this.orderItems = share.getOrderItems();
    }

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

    public String getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(String filterItems) {
        this.filterItems = filterItems;
    }

    public String getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(String orderItems) {
        this.orderItems = orderItems;
    }
}
