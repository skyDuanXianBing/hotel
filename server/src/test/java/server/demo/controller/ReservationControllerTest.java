package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.service.ReservationService;
import server.demo.dto.ReservationHoverSummaryResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest {

    @Test
    void getHoverSummaries_shouldReturnApiEnvelope() throws Exception {
        ReservationService reservationService = mock(ReservationService.class);
        ReservationController controller = new ReservationController();
        ReflectionTestUtils.setField(controller, "reservationService", reservationService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        ReservationHoverSummaryResponseDTO response = new ReservationHoverSummaryResponseDTO(
                new ReservationHoverSummaryResponseDTO.Capabilities(true, false),
                List.of(new ReservationHoverSummaryResponseDTO.Item(
                        101L, null, null, LocalDateTime.of(2026, 7, 12, 10, 0)))
        );
        when(reservationService.getHoverSummaries(List.of(101L, 102L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationIds\":[101,102]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取悬停摘要成功"))
                .andExpect(jsonPath("$.data.capabilities.guestPhone").value(true))
                .andExpect(jsonPath("$.data.capabilities.financial").value(false))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(101))
                .andExpect(jsonPath("$.data.items[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].paidAmount").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].updatedAt").exists());

        verify(reservationService).getHoverSummaries(List.of(101L, 102L));
    }

    @Test
    void getHoverSummaries_shouldRejectInvalidIds() throws Exception {
        ReservationService reservationService = mock(ReservationService.class);
        ReservationController controller = new ReservationController();
        ReflectionTestUtils.setField(controller, "reservationService", reservationService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationIds\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationIds\":[0]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationIds\":[null]}"))
                .andExpect(status().isBadRequest());

        String tooManyIds = LongStream.rangeClosed(1, 201)
                .mapToObj(Long::toString)
                .collect(Collectors.joining(","));
        mockMvc.perform(post("/api/v1/reservations/hover-summaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationIds\":[" + tooManyIds + "]}"))
                .andExpect(status().isBadRequest());
    }

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
