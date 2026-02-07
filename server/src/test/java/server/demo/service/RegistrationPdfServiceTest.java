package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.StoreRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RegistrationPdfServiceTest {

    @Test
    void render_shouldReturnPdfBytes() {
        RegistrationAttachmentRepository attRepo = Mockito.mock(RegistrationAttachmentRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        when(attRepo.findByFormId(1L)).thenReturn(List.of());

        RegistrationPdfService svc = new RegistrationPdfService("", attRepo, storeRepository);

        RegistrationForm form = new RegistrationForm();
        form.setId(1L);
        form.setOrderNumber("RSV_TEST_1");
        form.setStatus(null);

        Reservation reservation = new Reservation();
        reservation.setOrderNumber("RSV_TEST_1");
        reservation.setGuestName("Test Guest");
        reservation.setCheckInDate(LocalDate.of(2026, 2, 7));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 8));

        RegistrationGuest guest = new RegistrationGuest();
        guest.setId(1L);
        guest.setFirstName("Taro");
        guest.setLastName("Yamada");

        byte[] pdf = svc.render(form, reservation, List.of(guest));
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
