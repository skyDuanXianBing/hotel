package server.demo.dto.auth;

import server.demo.dto.StoreDTO;

public class CleanerContextDTO {
    private CleanerDTO cleaner;
    private StoreDTO store;
    public CleanerContextDTO() {}
    public CleanerContextDTO(CleanerDTO cleaner, StoreDTO store) { this.cleaner = cleaner; this.store = store; }
    public CleanerDTO getCleaner() { return cleaner; } public void setCleaner(CleanerDTO cleaner) { this.cleaner = cleaner; }
    public StoreDTO getStore() { return store; } public void setStore(StoreDTO store) { this.store = store; }
    public Long getCleanerId() { return cleaner == null ? null : cleaner.getId(); }
    public Long getStoreId() { return store == null ? (cleaner == null ? null : cleaner.getStoreId()) : store.getId(); }
}
