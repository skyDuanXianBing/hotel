package server.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 保洁员注册DTO
 */
public class CleanerRegistrationDTO {

    @NotBlank(message = "{api.t.0dda7fee4992}")
    private String token;

    @NotBlank(message = "{api.t.b0f8137f7e6b}")
    private String name;

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    @NotBlank(message = "{api.t.4d81424b0110}")
    @Size(min = 6, message = "{api.t.3bbd0a841187}")
    private String password;

    // Constructors
    public CleanerRegistrationDTO() {}

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
