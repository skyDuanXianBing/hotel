package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.service.ReservationService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest {

    @Test
    void getReservationsWithPagination_shouldParseAndPassOperationDate() throws Exception {
        ReservationService reservationService = mock(ReservationService.class);
        ReservationController controller = new ReservationController();
        ReflectionTestUtils.setField(controller, "reservationService", reservationService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/reservations")
                        .param("orderType", "today-new")
                        .param("operationDate", "2026-06-01"))
                .andExpect(status().isOk());

        verify(reservationService).getReservationsWithFilters(
                eq(0),
                eq(25),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("today-new"),
                eq(LocalDate.of(2026, 6, 1))
        );
    }
}
