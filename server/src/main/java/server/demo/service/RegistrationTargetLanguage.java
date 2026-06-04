package server.demo.service;

public class RegistrationTargetLanguage {
    public static final String DEFAULT_LANGUAGE_CODE = "en";
    public static final String DEFAULT_LANGUAGE_NAME = "English";

    private final String code;
    private final String name;
    private final boolean resolved;
    private final String source;
    private final String fallbackReason;

    private RegistrationTargetLanguage(
            String code,
            String name,
            boolean resolved,
            String source,
            String fallbackReason
    ) {
        this.code = code;
        this.name = name;
        this.resolved = resolved;
        this.source = source;
        this.fallbackReason = fallbackReason;
    }

    public static RegistrationTargetLanguage resolved(String code, String name, String source) {
        return new RegistrationTargetLanguage(code, name, true, source, null);
    }

    public static RegistrationTargetLanguage defaultEnglish(String fallbackReason) {
        return new RegistrationTargetLanguage(
                DEFAULT_LANGUAGE_CODE,
                DEFAULT_LANGUAGE_NAME,
                false,
                null,
                fallbackReason
        );
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getSource() {
        return source;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }
}
