package server.demo.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CleaningTaskDTO;
import server.demo.service.CleaningTaskAutoService;
import server.demo.service.CleaningTaskService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CleaningTaskControllerTest {

    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 15);
    private static final LocalDate END_DATE = LocalDate.of(2026, 6, 18);

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getTasks_shouldUseStoreWideListForAdminEvenWhenUserHasCleanerIdentity() throws Exception {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        CleaningTaskService cleaningTaskService = mock(CleaningTaskService.class);
        MockMvc mockMvc = mockMvc(cleaningTaskService);

        CleaningTaskDTO task = new CleaningTaskDTO();
        task.setId(501L);
        task.setReservationId(135L);
        task.setRoomId(33L);
        task.setRoomNumber("E2E-201");
        task.setTaskDate(LocalDate.of(2026, 6, 16));
        task.setTaskType("checkout");
        task.setSource("reservation");
        task.setStatus("pending");

        Page<CleaningTaskDTO> page = new PageImpl<>(List.of(task), PageRequest.of(0, 50), 1);
        when(cleaningTaskService.getTasksWithFilters(
                isNull(),
                eq(START_DATE),
                eq(END_DATE),
                ArgumentMatchers.<String>isNull(),
                eq("checkout"),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<String>isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/cleaning-tasks")
                        .requestAttr("userId", USER_ID)
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-18")
                        .param("taskType", "checkout")
                        .param("page", "0")
                        .param("size", "50")
                        .param("sortBy", "taskDate")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].reservationId").value(135))
                .andExpect(jsonPath("$.data.content[0].roomId").value(33))
                .andExpect(jsonPath("$.data.content[0].taskDate[0]").value(2026))
                .andExpect(jsonPath("$.data.content[0].taskDate[1]").value(6))
                .andExpect(jsonPath("$.data.content[0].taskDate[2]").value(16));

        verify(cleaningTaskService).getTasksWithFilters(
                isNull(),
                eq(START_DATE),
                eq(END_DATE),
                ArgumentMatchers.<String>isNull(),
                eq("checkout"),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<String>isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void getTasks_shouldKeepCleanerScopedListForMemberRole() throws Exception {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "member"));
        CleaningTaskService cleaningTaskService = mock(CleaningTaskService.class);
        MockMvc mockMvc = mockMvc(cleaningTaskService);

        when(cleaningTaskService.getTasksWithFilters(
                eq(USER_ID),
                eq(START_DATE),
                eq(END_DATE),
                ArgumentMatchers.<String>isNull(),
                eq("checkout"),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<String>isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/cleaning-tasks")
                        .requestAttr("userId", USER_ID)
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-18")
                        .param("taskType", "checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(cleaningTaskService).getTasksWithFilters(
                eq(USER_ID),
                eq(START_DATE),
                eq(END_DATE),
                ArgumentMatchers.<String>isNull(),
                eq("checkout"),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<Long>isNull(),
                ArgumentMatchers.<String>isNull(),
                any(Pageable.class)
        );
    }

    private MockMvc mockMvc(CleaningTaskService cleaningTaskService) {
        CleaningTaskController controller = new CleaningTaskController();
        ReflectionTestUtils.setField(controller, "cleaningTaskService", cleaningTaskService);
        ReflectionTestUtils.setField(controller, "cleaningTaskAutoService", mock(CleaningTaskAutoService.class));
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}
