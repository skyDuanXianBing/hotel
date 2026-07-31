package server.demo.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Wire {@link ApiMessages} for unit tests that call interceptors outside a Spring context.
 */
public final class TestApiMessages {

    private TestApiMessages() {
    }

    public static MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(I18nConfig.MESSAGE_BASENAMES);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(AppLocale.DEFAULT);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    public static void install() {
        ApiMessages.setService(new ApiMessageService(messageSource()));
    }
}
