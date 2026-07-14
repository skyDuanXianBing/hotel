package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SuMessagingTranslationSettingDTO {

    @NotNull(message = "翻译开关不能为空")
    private Boolean enabled;

    @NotNull(message = "目标语言不能为空")
    @Pattern(regexp = "^(zh-CN|en|ja|ko)$", message = "目标语言仅支持 zh-CN、en、ja、ko")
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
