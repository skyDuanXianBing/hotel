package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class UpsertPaymentMethodRequest {
    @NotBlank(message = "{api.t.4ec45d56edac}")
    private String name;

    private Boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
