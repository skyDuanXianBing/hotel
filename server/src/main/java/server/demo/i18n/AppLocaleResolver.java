package server.demo.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * Resolve order: X-App-Locale → Accept-Language → zh-CN.
 */
public class AppLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return AppLocale.resolve(
                request.getHeader(AppLocale.HEADER_APP_LOCALE),
                request.getHeader("Accept-Language")
        );
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // Request locale is header-driven; no session cookie persistence.
    }
}
