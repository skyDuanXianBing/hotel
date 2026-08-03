package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.AutoMessage;
import server.demo.entity.Reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuBusinessAutoMessageServiceGuestTemplateTest {

    private static final String DEFAULT_MESSAGE = "Hello {{guest_name}}, booking confirmed.";
    private static final String JA_MESSAGE = "{{guest_name}} 様、ご予約ありがとうございます。";

    private AutoMessage templateWithJa() {
        AutoMessage template = new AutoMessage();
        template.setMessage(DEFAULT_MESSAGE);
        template.setMessageJa(JA_MESSAGE);
        return template;
    }

    @Test
    void japaneseGuest_usesJapaneseTemplate() {
        Reservation reservation = new Reservation();
        reservation.setGuestCountry("JP");
        assertEquals(JA_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(reservation, templateWithJa()));

        Reservation byPhone = new Reservation();
        byPhone.setGuestPhone("+81-90-1234-5678");
        assertEquals(JA_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(byPhone, templateWithJa()));

        Reservation byLang = new Reservation();
        byLang.setGuestLanguage("ja");
        assertEquals(JA_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(byLang, templateWithJa()));
    }

    @Test
    void nonJapaneseGuest_usesDefaultTemplate() {
        Reservation reservation = new Reservation();
        reservation.setGuestCountry("US");
        reservation.setGuestPhone("+1-555-0100");
        assertEquals(DEFAULT_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(reservation, templateWithJa()));
    }

    @Test
    void japaneseGuestWithoutJaContent_fallsBackToDefault() {
        AutoMessage template = new AutoMessage();
        template.setMessage(DEFAULT_MESSAGE);

        Reservation reservation = new Reservation();
        reservation.setGuestCountry("JP");
        assertEquals(DEFAULT_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(reservation, template));

        // 日文内容为空白同样回退
        template.setMessageJa("   ");
        assertEquals(DEFAULT_MESSAGE,
                SuBusinessAutoMessageService.resolveTemplateContentForGuest(reservation, template));
    }

    @Test
    void nullTemplate_returnsNull() {
        assertNull(SuBusinessAutoMessageService.resolveTemplateContentForGuest(new Reservation(), null));
    }
}
