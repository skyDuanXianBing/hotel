package server.demo.dto.registration;

import java.util.List;

public class PublicRegistrationSaveRequest {
    private List<PublicRegistrationGuestDTO> guests;

    public List<PublicRegistrationGuestDTO> getGuests() {
        return guests;
    }

    public void setGuests(List<PublicRegistrationGuestDTO> guests) {
        this.guests = guests;
    }
}
