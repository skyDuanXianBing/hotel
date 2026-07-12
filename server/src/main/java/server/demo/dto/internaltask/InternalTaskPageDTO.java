package server.demo.dto.internaltask;

import java.util.List;

public class InternalTaskPageDTO {
    private final List<InternalTaskDTO> items; private final int page; private final int size;
    private final long totalElements; private final long assignedCount; private final long completedCount;
    public InternalTaskPageDTO(List<InternalTaskDTO> items, int page, int size, long totalElements,
                               long assignedCount, long completedCount) {
        this.items = items; this.page = page; this.size = size; this.totalElements = totalElements;
        this.assignedCount = assignedCount; this.completedCount = completedCount;
    }
    public List<InternalTaskDTO> getItems() { return items; } public int getPage() { return page; }
    public int getSize() { return size; } public long getTotalElements() { return totalElements; }
    public long getAssignedCount() { return assignedCount; } public long getCompletedCount() { return completedCount; }
}
