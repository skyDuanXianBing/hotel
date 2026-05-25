package server.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuWebhookConfigTest {

    @Test
    void reservationWebhookUrl_shouldUseServerBaseUrl() {
        SuWebhookConfig config = new SuWebhookConfig();
        ReflectionTestUtils.setField(config, "serverBaseUrl", "http://localhost:8092/");

        assertEquals(
                "http://localhost:8092/api/v1/su/webhook/reservation-notif/LOCALE2EHOTEL",
                config.getReservationNotifWebhookUrl("LOCALE2EHOTEL")
        );
        assertEquals(
                "http://localhost:8092/api/v1/su/webhook/reservation-notif",
                config.getReservationNotifWebhookUrl()
        );
    }
}
