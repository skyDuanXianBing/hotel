package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.dto.DailyRoomStatusDTO;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusSharePublicResponse;
import server.demo.entity.RoomStatusShare;
import server.demo.enums.RoomStatus;
import server.demo.repository.StoreRepository;
import server.demo.service.RoomStatusService;
import server.demo.service.RoomStatusShareService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomStatusShareControllerPublicPayloadTest {

    private static final String SHARE_TOKEN = "share-token";
    private static final Long SHARE_STORE_ID = 26L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 4, 1);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void publicShareDetailEndpoint_shouldReturnPublicConfigWithoutInternalMetadata() throws Exception {
        RoomStatusShareService shareService = mock(RoomStatusShareService.class);
        RoomStatusShareController controller = new RoomStatusShareController();
        ReflectionTestUtils.setField(controller, "roomStatusShareService", shareService);
        MockMvc mockMvc = mockMvc(controller);

        RoomStatusShare share = publicShare();
        when(shareService.getPublicShareByToken(SHARE_TOKEN))
                .thenReturn(new RoomStatusSharePublicResponse(share));

        MvcResult result = mockMvc.perform(get("/api/v1/room-status-share/public/{shareToken}", SHARE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shareTitle").value("BA public share"))
                .andExpect(jsonPath("$.data.viewRoomStatus").value(true))
                .andExpect(jsonPath("$.data.queryMethod").value(true))
                .andExpect(jsonPath("$.data.viewType").value("blurred"))
                .andExpect(jsonPath("$.data.queryMode").value("enabled"))
                .andExpect(jsonPath("$.data.filterItems").value("arrivals,available"))
                .andExpect(jsonPath("$.data.orderItems").value("guestName,channel"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.storeId").doesNotExist())
                .andExpect(jsonPath("$.data.associatedRoomIds").doesNotExist())
                .andExpect(jsonPath("$.data.shareToken").doesNotExist())
                .andExpect(jsonPath("$.data.shareLink").doesNotExist())
                .andReturn();

        JsonNode dataJson = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        assertEquals(7, dataJson.size());
        assertFalse(dataJson.has("id"));
        assertFalse(dataJson.has("userId"));
        assertFalse(dataJson.has("storeId"));
        assertFalse(dataJson.has("associatedRoomIds"));
        assertFalse(dataJson.has("shareToken"));
        assertFalse(dataJson.has("shareLink"));
    }

    @Test
    void publicRoomStatusEndpoint_shouldReturnSanitizedWhitelistPayloadWithoutAuth() throws Exception {
        RoomStatusShareService shareService = mock(RoomStatusShareService.class);
        RoomStatusShareService sanitizer = new RoomStatusShareService();
        RoomStatusService roomStatusService = mock(RoomStatusService.class);
        StoreRepository storeRepository = mock(StoreRepository.class);

        RoomStatusShareController controller = new RoomStatusShareController();
        ReflectionTestUtils.setField(controller, "roomStatusShareService", shareService);
        ReflectionTestUtils.setField(controller, "roomStatusService", roomStatusService);
        ReflectionTestUtils.setField(controller, "storeRepository", storeRepository);
        MockMvc mockMvc = mockMvc(controller);

        RoomStatusShare share = new RoomStatusShare();
        share.setUserId(7L);
        share.setStoreId(SHARE_STORE_ID);
        share.setViewType("normal");

        RoomStatusCalendarDTO internalCalendar = internalCalendar();
        when(shareService.getShareByToken(SHARE_TOKEN)).thenReturn(share);
        when(roomStatusService.getRoomStatusCalendarForStore(SHARE_STORE_ID, START_DATE, END_DATE))
                .thenReturn(internalCalendar);
        when(shareService.filterRoomStatusByShare(any(RoomStatusShare.class), any(RoomStatusCalendarDTO.class)))
                .thenAnswer(invocation -> sanitizer.filterRoomStatusByShare(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));

        MvcResult result = mockMvc.perform(get("/api/v1/room-status-share/public/{shareToken}/room-status", SHARE_TOKEN)
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rooms[0].roomId").value(1))
                .andExpect(jsonPath("$.data.rooms[0].dailyStatus[0].reservation.guestName").value("GuestA"))
                .andExpect(jsonPath("$.data.rooms[0].dailyStatus[0].reservation.channel").value("BOOKING"))
                .andExpect(jsonPath("$.data.rooms[0].dailyStatus[0].reservation.status").value("REQUESTED"))
                .andReturn();

        verify(roomStatusService).getRoomStatusCalendarForStore(SHARE_STORE_ID, START_DATE, END_DATE);

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode dailyStatusJson = root.at("/data/rooms/0/dailyStatus/0");
        JsonNode reservationJson = dailyStatusJson.get("reservation");

        assertEquals(5, reservationJson.size());
        assertFalse(reservationJson.has("id"));
        assertFalse(reservationJson.has("orderNumber"));
        assertFalse(reservationJson.has("channelOrderNumber"));
        assertFalse(reservationJson.has("groupOrderNo"));
        assertFalse(reservationJson.has("notes"));
        assertFalse(reservationJson.has("remark"));
        assertFalse(reservationJson.has("specialRequests"));
        assertFalse(reservationJson.has("totalAmount"));
        assertFalse(dailyStatusJson.has("closeRemark"));
    }

    private RoomStatusCalendarDTO internalCalendar() {
        DailyRoomStatusDTO.ReservationInfoDTO reservation =
                new DailyRoomStatusDTO.ReservationInfoDTO(
                        99L,
                        "GuestA",
                        "BOOKING",
                        START_DATE,
                        START_DATE.plusDays(1),
                        "ORD-1"
                );
        reservation.setStatus("REQUESTED");
        reservation.setTotalAmount(new BigDecimal("99.00"));
        reservation.setGroupOrderNo("GROUP-1");
        reservation.setNotes("private-note");
        reservation.setSpecialRequests("late arrival");

        DailyRoomStatusDTO dailyStatus = new DailyRoomStatusDTO(
                START_DATE,
                RoomStatus.RESERVED,
                reservation,
                Boolean.FALSE,
                "",
                "internal close remark"
        );

        RoomStatusCalendarDTO.CalendarRoomDataDTO room =
                new RoomStatusCalendarDTO.CalendarRoomDataDTO(
                        10L,
                        "101",
                        "标准房",
                        List.of(dailyStatus)
                );

        return new RoomStatusCalendarDTO(
                new RoomStatusCalendarDTO.DateRangeDTO(START_DATE, END_DATE),
                List.of(room)
        );
    }

    private RoomStatusShare publicShare() {
        RoomStatusShare share = new RoomStatusShare();
        share.setId(42L);
        share.setUserId(7L);
        share.setStoreId(SHARE_STORE_ID);
        share.setShareTitle("BA public share");
        share.setShareToken("internal-token");
        share.setShareLink("https://example.test/share/internal-token");
        share.setViewRoomStatus(true);
        share.setQueryMethod(true);
        share.setViewType("blurred");
        share.setQueryMode("enabled");
        share.setFilterItems("arrivals,available");
        share.setOrderItems("guestName,channel");
        share.setAssociatedRoomIds("101,102");
        return share;
    }

    private MockMvc mockMvc(RoomStatusShareController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setConversionService(new DefaultFormattingConversionService())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }
}
