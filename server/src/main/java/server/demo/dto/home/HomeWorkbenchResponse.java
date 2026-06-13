package server.demo.dto.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HomeWorkbenchResponse {
    private LocalDate businessDate;
    private LocalDateTime generatedAt;
    private List<HomeWorkbenchTypeSummaryDTO> typeSummaries = new ArrayList<>();
    private List<HomeWorkbenchStatusSummaryDTO> statusSummaries = new ArrayList<>();
    private List<HomeWorkbenchItemDTO> items = new ArrayList<>();

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<HomeWorkbenchTypeSummaryDTO> getTypeSummaries() {
        return typeSummaries;
    }

    public void setTypeSummaries(List<HomeWorkbenchTypeSummaryDTO> typeSummaries) {
        this.typeSummaries = typeSummaries;
    }

    public List<HomeWorkbenchStatusSummaryDTO> getStatusSummaries() {
        return statusSummaries;
    }

    public void setStatusSummaries(List<HomeWorkbenchStatusSummaryDTO> statusSummaries) {
        this.statusSummaries = statusSummaries;
    }

    public List<HomeWorkbenchItemDTO> getItems() {
        return items;
    }

    public void setItems(List<HomeWorkbenchItemDTO> items) {
        this.items = items;
    }
}
