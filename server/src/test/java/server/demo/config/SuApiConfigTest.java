package server.demo.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuApiConfigTest {

    @Test
    void shouldUseExplicitBaseUrlWhenConfigured() {
        SuApiConfig config = new SuApiConfig();
        config.setEnv("production");
        config.setBaseUrl(" https://custom.su-api.example.com ");

        assertEquals("https://custom.su-api.example.com", config.getBaseUrl());
    }

    @Test
    void shouldResolveProductionBaseUrlByEnv() {
        SuApiConfig config = new SuApiConfig();
        config.setEnv("production");
        config.setBaseUrl(" ");

        assertEquals("https://connect.su-api.com", config.getBaseUrl());
    }

    @Test
    void shouldResolveSandboxBaseUrlByEnv() {
        SuApiConfig config = new SuApiConfig();
        config.setEnv("sandbox");

        assertEquals("https://connect-sandbox.su-api.com", config.getBaseUrl());
    }

    @Test
    void shouldDefaultToSandboxWhenEnvIsMissing() {
        SuApiConfig config = new SuApiConfig();

        assertEquals("https://connect-sandbox.su-api.com", config.getBaseUrl());
    }

    @Test
    void shouldThrowForInvalidEnv() {
        SuApiConfig config = new SuApiConfig();
        config.setEnv("prod");
        config.setBaseUrl("");

        assertThrows(IllegalStateException.class, config::getBaseUrl);
    }
}
