package server.demo.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApiMessageServiceTest {

    private ApiMessageService apiMessageService;

    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(I18nConfig.MESSAGE_BASENAMES);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(AppLocale.DEFAULT);
        messageSource.setUseCodeAsDefaultMessage(true);
        apiMessageService = new ApiMessageService(messageSource);
        ApiMessages.setService(apiMessageService);
    }

    @Test
    void resolve_commonSuccessInZhCnAndEn() {
        assertEquals("操作成功", apiMessageService.resolve(Locale.forLanguageTag("zh-CN"), "api.common.success"));
        assertEquals("Operation successful", apiMessageService.resolve(Locale.forLanguageTag("en"), "api.common.success"));
    }

    @Test
    void resolve_authTokenMissingFollowsLocaleContext() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ja"));
        try {
            String message = ApiMessages.get("api.auth.token.missing");
            assertFalse(message.contains("未提供"));
            assertEquals("認証トークンが提供されていません", message);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void resolve_traditionalChinese() {
        String message = apiMessageService.resolve(Locale.forLanguageTag("zh-TW"), "api.auth.token.invalid");
        assertEquals("認證令牌無效或已過期", message);
    }
}
