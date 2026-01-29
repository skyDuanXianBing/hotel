package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class ForceDeleteRequest {
    @NotNull
    private Boolean confirm;

    public Boolean getConfirm() {
        return confirm;
    }

    public void setConfirm(Boolean confirm) {
        this.confirm = confirm;
    }
}

