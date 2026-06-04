package server.demo.service;

public class AiTranslationResult {
    private final String translatedText;
    private final RegistrationTargetLanguage targetLanguage;
    private final boolean translated;
    private final String errorMessage;

    private AiTranslationResult(
            String translatedText,
            RegistrationTargetLanguage targetLanguage,
            boolean translated,
            String errorMessage
    ) {
        this.translatedText = translatedText;
        this.targetLanguage = targetLanguage;
        this.translated = translated;
        this.errorMessage = errorMessage;
    }

    public static AiTranslationResult translated(String text, RegistrationTargetLanguage targetLanguage) {
        return new AiTranslationResult(text, targetLanguage, true, null);
    }

    public static AiTranslationResult fallback(
            String originalText,
            RegistrationTargetLanguage targetLanguage,
            String errorMessage
    ) {
        return new AiTranslationResult(originalText, targetLanguage, false, errorMessage);
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public RegistrationTargetLanguage getTargetLanguage() {
        return targetLanguage;
    }

    public boolean isTranslated() {
        return translated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
