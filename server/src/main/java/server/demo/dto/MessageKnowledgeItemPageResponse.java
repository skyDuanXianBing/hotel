package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MessageKnowledgeItemPageResponse {
    private List<MessageKnowledgeItemDTO> content = new ArrayList<>();
    private int page;
    private int number;
    private int size;
    private long totalElements;
    private long total;
    private int totalPages;
    private boolean hasNext;

    public MessageKnowledgeItemPageResponse() {
    }

    public MessageKnowledgeItemPageResponse(
            List<MessageKnowledgeItemDTO> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        setContent(content);
        this.page = page;
        this.number = page;
        this.size = size;
        this.totalElements = totalElements;
        this.total = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    public List<MessageKnowledgeItemDTO> getContent() {
        return content;
    }

    public void setContent(List<MessageKnowledgeItemDTO> content) {
        this.content = content == null ? new ArrayList<>() : content;
    }

    public List<MessageKnowledgeItemDTO> getItems() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
