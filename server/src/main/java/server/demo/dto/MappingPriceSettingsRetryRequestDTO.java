package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MappingPriceSettingsRetryRequestDTO {

    private String clientOperationId;
    private List<String> rowKeys = new ArrayList<>();

    public String getClientOperationId() {
        return clientOperationId;
    }

    public void setClientOperationId(String clientOperationId) {
        this.clientOperationId = clientOperationId;
    }

    public List<String> getRowKeys() {
        return rowKeys;
    }

    public void setRowKeys(List<String> rowKeys) {
        this.rowKeys = rowKeys != null ? rowKeys : new ArrayList<>();
    }
}
