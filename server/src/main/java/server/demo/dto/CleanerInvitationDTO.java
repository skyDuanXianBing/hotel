package server.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 保洁员邀请请求DTO
 */
public class CleanerInvitationDTO {

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    @NotBlank(message = "{api.t.b0f8137f7e6b}")
    private String name;

    private Long userId;

    private Long storeId;

    // Constructors
    public CleanerInvitationDTO() {}

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
