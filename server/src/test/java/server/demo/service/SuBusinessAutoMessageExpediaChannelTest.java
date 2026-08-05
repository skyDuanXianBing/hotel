package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.AutoMessage;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4 聚焦测试：订单事件类自动消息（BOOKING_CONFIRM/CHECK_IN/CHECK_OUT 等）的投递通道判定。
 * 投递经 Su OTA Messages API（postMessagingAB），因此渠道映射按目录 messaging 能力集放行：
 * EXPEDIA(9) 放行；TRIP(339)/AGODA(189) 官方不支持消息，保持排除。
 */
class SuBusinessAutoMessageExpediaChannelTest {

    private final SuBusinessAutoMessageService service = new SuBusinessAutoMessageService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Mockito.mock(SuApiClient.class),
            null,
            null,
            new ObjectMapper(),
            null,
            "http://localhost:8091/",
            "Auto Message"
    );

    @Test
    void shouldSend_allowsExpediaReservations() {
        Reservation reservation = reservationWithChannel(66L, "EXPEDIA");
        AutoMessage template = templateMatching(66L);

        SuBusinessAutoMessageService.DispatchDecision decision = service.shouldSend(reservation, template);

        assertTrue(decision.okToSend());
        assertEquals("ok", decision.reason());
    }

    @Test
    void shouldSend_stillAllowsBookingAndAirbnb() {
        assertTrue(service.shouldSend(reservationWithChannel(33L, "BOOKING"), templateMatching(33L)).okToSend());
        assertTrue(service.shouldSend(reservationWithChannel(44L, "AIRBNB"), templateMatching(44L)).okToSend());
        assertTrue(service.shouldSend(reservationWithChannel(33L, "BOOKING.COM"), templateMatching(33L)).okToSend());
    }

    @Test
    void shouldSend_rejectsTripAndAgodaBecauseSuMessagingUnsupported() {
        SuBusinessAutoMessageService.DispatchDecision trip =
                service.shouldSend(reservationWithChannel(77L, "TRIP"), templateMatching(77L));
        SuBusinessAutoMessageService.DispatchDecision ctrip =
                service.shouldSend(reservationWithChannel(77L, "CTRIP"), templateMatching(77L));
        SuBusinessAutoMessageService.DispatchDecision agoda =
                service.shouldSend(reservationWithChannel(78L, "AGODA"), templateMatching(78L));

        assertFalse(trip.okToSend());
        assertFalse(ctrip.okToSend());
        assertFalse(agoda.okToSend());
        assertEquals("unsupported channel (Su messaging supports 19/244/9 only)", trip.reason());
        assertEquals("unsupported channel (Su messaging supports 19/244/9 only)", agoda.reason());
    }

    private static Reservation reservationWithChannel(Long channelId, String channelCode) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setStoreId(10L);
        channel.setCode(channelCode);

        Reservation reservation = new Reservation();
        reservation.setId(900L);
        reservation.setStoreId(10L);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setChannel(channel);
        return reservation;
    }

    private static AutoMessage templateMatching(Long channelId) {
        AutoMessage template = new AutoMessage();
        template.setId(88L);
        template.setStoreId(10L);
        template.setChannels("[" + channelId + "]");
        template.setRoomSelectionType("ALL_LOCAL");
        return template;
    }
}
