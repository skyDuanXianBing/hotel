package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.annotation.StoreScoped;
import server.demo.dto.RoomTableMonthlyResponse;
import server.demo.service.RoomTableService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomTableControllerMonthlyTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 4, 2);
    private static final Long ROOM_TYPE_ID = 10L;

    @Test
    void monthlyEndpoint_shouldReturnEnvelopeAndPassScopedParametersToService() throws Exception {
        assertTrue(RoomTableController.class.isAnnotationPresent(StoreScoped.class));

        RoomTableService roomTableService = mock(RoomTableService.class);
        RoomTableController controller = new RoomTableController();
        ReflectionTestUtils.setField(controller, "roomTableService", roomTableService);
        MockMvc mockMvc = mockMvc(controller);

        when(roomTableService.getMonthlyRoomTable(START_DATE, END_DATE, ROOM_TYPE_ID))
                .thenReturn(monthlyResponse());

        mockMvc.perform(get("/api/v1/room-table/monthly")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-02")
                        .param("roomTypeId", ROOM_TYPE_ID.toString())
                        .param("storeId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-04-02"))
                .andExpect(jsonPath("$.data.rooms[0].roomId").value(101))
                .andExpect(jsonPath("$.data.rooms[0].dailyStatus[0].displayStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.roomTypeSummaries[0].effectiveAvailableRooms").value(1));

        verify(roomTableService).getMonthlyRoomTable(
                eq(START_DATE),
                eq(END_DATE),
                eq(ROOM_TYPE_ID)
        );
    }

    private RoomTableMonthlyResponse monthlyResponse() {
        RoomTableMonthlyResponse.MonthlyDailyStatusDTO dailyStatus =
                new RoomTableMonthlyResponse.MonthlyDailyStatusDTO();
        dailyStatus.setDate(START_DATE);
        dailyStatus.setStatus("AVAILABLE");
        dailyStatus.setDisplayStatus("AVAILABLE");
        dailyStatus.setSellable(true);

        RoomTableMonthlyResponse.MonthlyRoomDataDTO room =
                new RoomTableMonthlyResponse.MonthlyRoomDataDTO(
                        101L,
                        "101",
                        ROOM_TYPE_ID,
                        "标准房",
                        List.of(dailyStatus)
                );

        RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO summary =
                new RoomTableMonthlyResponse.MonthlyRoomTypeSummaryDTO();
        summary.setRoomTypeId(ROOM_TYPE_ID);
        summary.setRoomTypeName("标准房");
        summary.setDate(START_DATE);
        summary.setTotalRooms(2);
        summary.setEffectiveAvailableRooms(1);

        return new RoomTableMonthlyResponse(
                START_DATE,
                END_DATE,
                List.of(room),
                List.of(summary)
        );
    }

    private MockMvc mockMvc(RoomTableController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(new DefaultFormattingConversionService())
                .setMessageConverters(jsonConverter())
                .build();
    }

    private MappingJackson2HttpMessageConverter jsonConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
