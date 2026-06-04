package server.demo.service;

public interface AiTranslationService {
    AiTranslationResult translate(String sourceText, RegistrationTargetLanguage targetLanguage);
}
