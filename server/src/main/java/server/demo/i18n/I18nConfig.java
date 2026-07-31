package server.demo.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class I18nConfig {

    /**
     * MessageSource basenames under {@code classpath:i18n/}.
     * Shared {@code messages} first, then domain bundles. Do not duplicate keys across basenames.
     */
    static final String[] MESSAGE_BASENAMES = {
            "classpath:i18n/messages",
            "classpath:i18n/messages_auth",
            "classpath:i18n/messages_validation",
            "classpath:i18n/messages_reservation",
            "classpath:i18n/messages_room",
            "classpath:i18n/messages_channel",
            "classpath:i18n/messages_billing",
            "classpath:i18n/messages_cleaning",
            "classpath:i18n/messages_admin",
            "classpath:i18n/messages_registration",
            "classpath:i18n/messages_lock",
            "classpath:i18n/messages_site",
            "classpath:i18n/messages_message",
            "classpath:i18n/messages_notification",
            "classpath:i18n/messages_store",
            "classpath:i18n/messages_misc"
    };

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(MESSAGE_BASENAMES);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(AppLocale.DEFAULT);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public ApiMessageService apiMessageService(MessageSource messageSource) {
        ApiMessageService service = new ApiMessageService(messageSource);
        ApiMessages.setService(service);
        return service;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new AppLocaleResolver();
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setValidationMessageSource(messageSource);
        return factoryBean;
    }
}
