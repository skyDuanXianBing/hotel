package server.demo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ChannelMappingMultiplierSyncSummaryDTO {

    private String channelCode;
    private String suChannelId;
    private String hotelId;
    private BigDecimal requestedMultiplier;
    private BigDecimal requestedSurcharge;
    private String status;
    private String message;
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<Item> items = new ArrayList<>();

    public static ChannelMappingMultiplierSyncSummaryDTO skipped(
            String channelCode,
            String message
    ) {
        ChannelMappingMultiplierSyncSummaryDTO dto = new ChannelMappingMultiplierSyncSummaryDTO();
        dto.setChannelCode(channelCode);
        dto.setStatus("SKIPPED");
        dto.setMessage(message);
        return dto;
    }

    public void refreshCounts() {
        int success = 0;
        int failure = 0;
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            if ("SUCCESS".equalsIgnoreCase(item.getStatus())) {
                success++;
            } else if ("FAILED".equalsIgnoreCase(item.getStatus())) {
                failure++;
            }
        }
        this.totalCount = items.size();
        this.successCount = success;
        this.failureCount = failure;
        if (totalCount == 0) {
            this.status = "FAILED";
        } else if (failure == 0) {
            this.status = "SUCCESS";
        } else if (success == 0) {
            this.status = "FAILED";
        } else {
            this.status = "PARTIAL";
        }
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getSuChannelId() {
        return suChannelId;
    }

    public void setSuChannelId(String suChannelId) {
        this.suChannelId = suChannelId;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public BigDecimal getRequestedMultiplier() {
        return requestedMultiplier;
    }

    public void setRequestedMultiplier(BigDecimal requestedMultiplier) {
        this.requestedMultiplier = requestedMultiplier;
    }

    public BigDecimal getRequestedSurcharge() {
        return requestedSurcharge;
    }

    public void setRequestedSurcharge(BigDecimal requestedSurcharge) {
        this.requestedSurcharge = requestedSurcharge;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public static class Item {
        private String status;
        private String channelHotelId;
        private String roomId;
        private String rateId;
        private String channelRoomId;
        private String channelRateId;
        private String listingId;
        private String applicableNoOfGuest;
        private String message;

        public static Item success(MappingRef ref, String message) {
            Item item = from(ref);
            item.setStatus("SUCCESS");
            item.setMessage(message);
            return item;
        }

        public static Item failure(MappingRef ref, String message) {
            Item item = from(ref);
            item.setStatus("FAILED");
            item.setMessage(message);
            return item;
        }

        private static Item from(MappingRef ref) {
            Item item = new Item();
            if (ref == null) {
                return item;
            }
            item.setChannelHotelId(ref.channelHotelId());
            item.setRoomId(ref.roomId());
            item.setRateId(ref.rateId());
            item.setChannelRoomId(ref.channelRoomId());
            item.setChannelRateId(ref.channelRateId());
            item.setListingId(ref.listingId());
            item.setApplicableNoOfGuest(ref.applicableNoOfGuest());
            return item;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getChannelHotelId() {
            return channelHotelId;
        }

        public void setChannelHotelId(String channelHotelId) {
            this.channelHotelId = channelHotelId;
        }

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getRateId() {
            return rateId;
        }

        public void setRateId(String rateId) {
            this.rateId = rateId;
        }

        public String getChannelRoomId() {
            return channelRoomId;
        }

        public void setChannelRoomId(String channelRoomId) {
            this.channelRoomId = channelRoomId;
        }

        public String getChannelRateId() {
            return channelRateId;
        }

        public void setChannelRateId(String channelRateId) {
            this.channelRateId = channelRateId;
        }

        public String getListingId() {
            return listingId;
        }

        public void setListingId(String listingId) {
            this.listingId = listingId;
        }

        public String getApplicableNoOfGuest() {
            return applicableNoOfGuest;
        }

        public void setApplicableNoOfGuest(String applicableNoOfGuest) {
            this.applicableNoOfGuest = applicableNoOfGuest;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public record MappingRef(
            String channelHotelId,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId,
            String listingId,
            String applicableNoOfGuest
    ) {}
}
