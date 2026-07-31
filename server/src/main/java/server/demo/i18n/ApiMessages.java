package server.demo.i18n;

/**
 * Static facade for resolving localized API messages from request threads.
 * Wired by {@link I18nConfig} at startup.
 */
public final class ApiMessages {

    private static volatile ApiMessageService service;

    private ApiMessages() {
    }

    static void setService(ApiMessageService apiMessageService) {
        service = apiMessageService;
    }

    public static String get(String key, Object... args) {
        ApiMessageService current = service;
        if (current == null) {
            return key;
        }
        return current.resolve(key, args);
    }
}
