package server.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtaReservationSyncServiceRoomReservationIdentityTest {

    @Test
    void resolveRoomReservationIdentity_shouldUseRawIdWhenPresent() {
        String actual = OtaReservationSyncService.resolveRoomReservationIdentity(" rr-1001 ", 0);
        assertEquals("rr-1001", actual);
    }

    @Test
    void resolveRoomReservationIdentity_shouldFallbackToStableIndexIdentity() {
        assertEquals("IDX1", OtaReservationSyncService.resolveRoomReservationIdentity(null, 0));
        assertEquals("IDX3", OtaReservationSyncService.resolveRoomReservationIdentity(" ", 2));
    }
}

