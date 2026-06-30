package server.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class MappingPriceSettingsSaveRequestDTO {

    private String clientOperationId;
    private List<MappingPriceSettingRowSaveRequestDTO> rows = new ArrayList<>();

    public String getClientOperationId() {
        return clientOperationId;
    }

    public void setClientOperationId(String clientOperationId) {
        this.clientOperationId = clientOperationId;
    }

    public List<MappingPriceSettingRowSaveRequestDTO> getRows() {
        return rows;
    }

    public void setRows(List<MappingPriceSettingRowSaveRequestDTO> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
