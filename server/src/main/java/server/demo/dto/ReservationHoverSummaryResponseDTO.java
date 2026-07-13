package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationHoverSummaryResponseDTO {

    private Capabilities capabilities;
    private List<Item> items;

    public ReservationHoverSummaryResponseDTO() {
    }

    public ReservationHoverSummaryResponseDTO(Capabilities capabilities, List<Item> items) {
        this.capabilities = capabilities;
        this.items = items;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Capabilities {
        private boolean guestPhone;
        private boolean financial;

        public Capabilities() {
        }

        public Capabilities(boolean guestPhone, boolean financial) {
            this.guestPhone = guestPhone;
            this.financial = financial;
        }

        public boolean isGuestPhone() {
            return guestPhone;
        }

        public void setGuestPhone(boolean guestPhone) {
            this.guestPhone = guestPhone;
        }

        public boolean isFinancial() {
            return financial;
        }

        public void setFinancial(boolean financial) {
            this.financial = financial;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private Long reservationId;
        private String phone;
        private BigDecimal paidAmount;
        private LocalDateTime updatedAt;

        public Item() {
        }

        public Item(Long reservationId, String phone, BigDecimal paidAmount, LocalDateTime updatedAt) {
            this.reservationId = reservationId;
            this.phone = phone;
            this.paidAmount = paidAmount;
            this.updatedAt = updatedAt;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public BigDecimal getPaidAmount() {
            return paidAmount;
        }

        public void setPaidAmount(BigDecimal paidAmount) {
            this.paidAmount = paidAmount;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
