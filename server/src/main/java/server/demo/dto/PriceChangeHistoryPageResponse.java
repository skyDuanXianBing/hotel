package server.demo.dto;

import java.util.List;

/**
 * 改价历史分页响应
 */
public class PriceChangeHistoryPageResponse {
    private Long total;
    private List<PriceChangeHistoryDTO> records;
    private Integer pageNum;
    private Integer pageSize;

    // Constructors
    public PriceChangeHistoryPageResponse() {}

    public PriceChangeHistoryPageResponse(Long total, List<PriceChangeHistoryDTO> records, Integer pageNum, Integer pageSize) {
        this.total = total;
        this.records = records;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<PriceChangeHistoryDTO> getRecords() {
        return records;
    }

    public void setRecords(List<PriceChangeHistoryDTO> records) {
        this.records = records;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
