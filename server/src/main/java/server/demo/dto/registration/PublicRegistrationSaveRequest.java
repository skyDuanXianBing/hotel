package server.demo.dto.registration;

import java.util.List;

public class PublicRegistrationSaveRequest {
    private Integer guestCount;
    private List<PublicRegistrationGuestDTO> guests;

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public List<PublicRegistrationGuestDTO> getGuests() {
        return guests;
    }

    public void setGuests(List<PublicRegistrationGuestDTO> guests) {
        this.guests = guests;
    }
}
