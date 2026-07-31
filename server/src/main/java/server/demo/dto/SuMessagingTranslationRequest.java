package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class SuMessagingTranslationRequest {
    @NotBlank(message = "{api.t.460ce9217fb8}")
    private String targetLanguage;

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
}
