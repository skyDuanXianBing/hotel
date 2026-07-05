package server.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.demo.service.StatisticsReportExportService;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsReportControllerTest {

    @Test
    void exportReport_unsupportedTypeShouldReturnBadRequestJsonEnvelope() throws Exception {
        StatisticsReportExportService reportExportService = mock(StatisticsReportExportService.class);
        when(reportExportService.isSupported("unsupported")).thenReturn(false);
        when(reportExportService.supportedTypes()).thenReturn(Set.of("room-fees", "daily"));
        MockMvc mockMvc = mockMvc(reportExportService);

        mockMvc.perform(get("/api/v1/statistics/reports/unsupported")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-02"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("不支持的报表类型")));
    }

    @Test
    void exportReport_invalidDateRangeShouldReturnBadRequestJsonEnvelope() throws Exception {
        StatisticsReportExportService reportExportService = mock(StatisticsReportExportService.class);
        MockMvc mockMvc = mockMvc(reportExportService);

        mockMvc.perform(get("/api/v1/statistics/reports/daily")
                        .param("startDate", "2026-02-03")
                        .param("endDate", "2026-02-02"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("开始日期不能晚于结束日期"));
    }

    @Test
    void exportReport_supportedTypeShouldReturnCsvAndDownloadHeader() throws Exception {
        StatisticsReportExportService reportExportService = mock(StatisticsReportExportService.class);
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 2);
        when(reportExportService.isSupported("daily")).thenReturn(true);
        when(reportExportService.exportCsv("daily", startDate, endDate, null, null, null))
                .thenReturn("date,total\n2026-02-01,100\n");
        MockMvc mockMvc = mockMvc(reportExportService);

        mockMvc.perform(get("/api/v1/statistics/reports/daily")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-02"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"daily-2026-02-01-2026-02-02.csv\"")))
                .andExpect(content().string("date,total\n2026-02-01,100\n"));
    }

    private MockMvc mockMvc(StatisticsReportExportService reportExportService) {
        StatisticsReportController controller = new StatisticsReportController(reportExportService);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}
