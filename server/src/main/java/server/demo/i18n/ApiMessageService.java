package server.demo.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * Resolves API user-facing messages from MessageSource for the current request locale.
 */
public class ApiMessageService {

    private final MessageSource messageSource;

    public ApiMessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(String key, Object... args) {
        return resolve(LocaleContextHolder.getLocale(), key, args);
    }

    public String resolve(Locale locale, String key, Object... args) {
        Locale effective = locale != null ? AppLocale.normalize(locale) : null;
        if (effective == null) {
            effective = AppLocale.DEFAULT;
        }
        Object[] messageArgs = args == null ? new Object[0] : args;
        return messageSource.getMessage(key, messageArgs, key, effective);
    }
}
