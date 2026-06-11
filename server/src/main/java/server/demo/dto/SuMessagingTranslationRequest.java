package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class SuMessagingTranslationRequest {
    @NotBlank(message = "目标语言不能为空")
    private String targetLanguage;

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
}
