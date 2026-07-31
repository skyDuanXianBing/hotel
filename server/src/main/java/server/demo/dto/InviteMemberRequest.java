package server.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Invite member request DTO
 */
public class InviteMemberRequest {

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    @NotBlank(message = "{api.t.f4d7a9c76e6e}")
    @Pattern(regexp = "^(admin|member)$", message = "{api.t.95e040f355e7}")
    private String role;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
