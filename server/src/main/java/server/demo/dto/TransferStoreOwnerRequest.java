package server.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更换门店负责人的请求。
 */
public class TransferStoreOwnerRequest {

    @NotNull(message = "新负责人不能为空")
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}
