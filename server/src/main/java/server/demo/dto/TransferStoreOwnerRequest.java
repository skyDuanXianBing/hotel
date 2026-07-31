package server.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更换门店负责人的请求。
 */
public class TransferStoreOwnerRequest {

    @NotNull(message = "{api.t.3823a21a1ddf}")
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}
