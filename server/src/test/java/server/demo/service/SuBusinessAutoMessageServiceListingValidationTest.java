package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.Reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuBusinessAutoMessageServiceListingValidationTest {

    @Test
    void resolveTrustedListingId_rejectsShortNumericRoomIds() {
        assertNull(SuBusinessAutoMessageService.resolveTrustedListingId("50"));
        assertNull(SuBusinessAutoMessageService.resolveTrustedListingId(" 51 "));
    }

    @Test
    void resolveTrustedListingId_acceptsValidMessagingListingIds() {
        assertEquals("16016360", SuBusinessAutoMessageService.resolveTrustedListingId("16016360"));
        assertEquals("ABCD_1234", SuBusinessAutoMessageService.resolveTrustedListingId("ABCD_1234"));
    }

    @Test
    void resolveThreadBookingIdForAutoMessage_bookingFallsBackToOrderNumberPattern() {
        Reservation reservation = new Reservation();
        reservation.setOrderNumber("SU26-5003249282_W39FVCQYSN-1774939615039");

        assertEquals(
                "5003249282",
                SuBusinessAutoMessageService.resolveThreadBookingIdForAutoMessage(
                        reservation,
                        SuMessagingService.CHANNEL_BOOKING
                )
        );
    }

    @Test
    void canCreateBookingFallbackThread_requiresSuSourceIdentifiers() {
        Reservation reservation = new Reservation();
        assertFalse(SuBusinessAutoMessageService.canCreateBookingFallbackThread(reservation));

        reservation.setReservationNotifId("177487870881569582319");
        assertTrue(SuBusinessAutoMessageService.canCreateBookingFallbackThread(reservation));
    }

    @Test
    void classifyRecoverableWaitCode_invalidReservationIdMapsToThreadFields() {
        String error = "WAITING_LISTINGID: SU_ERR[code=UNKNOWN]: Invalid ReservationID {channelId=19, threadId=SU26-5003249282_W39-1774939615039, bookingId=SU26-5003249282_W39-1774939615039, listingId=16016360}";

        assertEquals(
                "WAITING_THREAD_FIELDS",
                SuBusinessAutoMessageService.classifyRecoverableWaitCode(error)
        );
    }
}
