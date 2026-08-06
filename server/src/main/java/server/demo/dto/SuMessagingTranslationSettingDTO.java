package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SuMessagingTranslationSettingDTO {

    @NotNull(message = "{api.t.21c6d360e510}")
    private Boolean enabled;

    @NotNull(message = "{api.t.460ce9217fb8}")
    @Pattern(regexp = "^(zh-CN|zh-TW|en|ja)$", message = "{api.t.703096170de5}")
    private String targetLanguage;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean configured;

    public SuMessagingTranslationSettingDTO() {
    }

    public SuMessagingTranslationSettingDTO(Boolean enabled, String targetLanguage) {
        this(enabled, targetLanguage, null);
    }

    public SuMessagingTranslationSettingDTO(Boolean enabled, String targetLanguage, Boolean configured) {
        this.enabled = enabled;
        this.targetLanguage = targetLanguage;
        this.configured = configured;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public Boolean getConfigured() {
        return configured;
    }
}
