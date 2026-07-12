package server.demo.dto.home;

public class HomeWorkbenchPageDTO {
    private int size;
    private int returnedElements;
    private Long totalElements;
    private boolean hasMore;
    private String nextCursor;

    public HomeWorkbenchPageDTO() {
    }

    public HomeWorkbenchPageDTO(int size, int returnedElements, Long totalElements,
                                boolean hasMore, String nextCursor) {
        this.size = size;
        this.returnedElements = returnedElements;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
        this.nextCursor = nextCursor;
    }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getReturnedElements() { return returnedElements; }
    public void setReturnedElements(int returnedElements) { this.returnedElements = returnedElements; }
    public Long getTotalElements() { return totalElements; }
    public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}
