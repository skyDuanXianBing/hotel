package server.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelE2ELocalConfigBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(ChannelE2ELocalConfigBindingTest::loadApplicationProperties)
            .withUserConfiguration(TestConfig.class);

    @Test
    void unifiedLocalE2EFlagEnablesLocalMockAndTestSupportFlag() {
        contextRunner
                .withPropertyValues(
                        "CHANNEL_E2E_LOCAL_ENABLED=true",
                        "SU_API_LOCAL_MOCK_BASE_URL=http://localhost:4000",
                        "SU_API_LOCAL_MOCK_CLIENT_ID=mock-client-id",
                        "SU_API_LOCAL_MOCK_CLIENT_SECRET=mock-client-secret"
                )
                .run(context -> {
                    SuApiConfig suApiConfig = context.getBean(SuApiConfig.class);
                    ChannelE2EFlagProbe probe = context.getBean(ChannelE2EFlagProbe.class);

                    assertEquals("true", context.getEnvironment().getProperty("channel.e2e.local-enabled"));
                    assertTrue(suApiConfig.isLocalMockEnabled());
                    assertEquals("http://localhost:4000", suApiConfig.getBaseUrl());
                    assertEquals("mock-client-id", suApiConfig.getClientId());
                    assertEquals("mock-client-secret", suApiConfig.getClientSecret());
                    assertTrue(probe.isLocalEnabled());
                });
    }

    @Test
    void suLocalMockEnvDoesNotEnableLocalMockWithoutUnifiedLocalE2EFlag() {
        contextRunner
                .withPropertyValues(
                        "CHANNEL_E2E_LOCAL_ENABLED=false",
                        "SU_API_LOCAL_MOCK_ENABLED=true",
                        "SU_API_BASE_URL=https://connect.su-api.com",
                        "SU_CLIENT_ID=production-client-id",
                        "SU_CLIENT_SECRET=production-secret",
                        "SU_API_LOCAL_MOCK_BASE_URL=http://localhost:4000",
                        "SU_API_LOCAL_MOCK_CLIENT_ID=mock-client-id",
                        "SU_API_LOCAL_MOCK_CLIENT_SECRET=mock-client-secret"
                )
                .run(context -> {
                    SuApiConfig suApiConfig = context.getBean(SuApiConfig.class);
                    ChannelE2EFlagProbe probe = context.getBean(ChannelE2EFlagProbe.class);

                    assertFalse(probe.isLocalEnabled());
                    assertFalse(suApiConfig.isLocalMockEnabled());
                    assertEquals("https://connect.su-api.com", suApiConfig.getBaseUrl());
                    assertEquals("production-client-id", suApiConfig.getClientId());
                    assertEquals("production-secret", suApiConfig.getClientSecret());
                });
    }

    @Test
    void localE2EFlagsDefaultToDisabled() {
        contextRunner.run(context -> {
            SuApiConfig suApiConfig = context.getBean(SuApiConfig.class);
            ChannelE2EFlagProbe probe = context.getBean(ChannelE2EFlagProbe.class);

            assertFalse(suApiConfig.isLocalMockEnabled());
            assertFalse(probe.isLocalEnabled());
        });
    }

    private static void loadApplicationProperties(
            org.springframework.context.ConfigurableApplicationContext context
    ) {
        try {
            ConfigurableEnvironment environment = context.getEnvironment();
            ResourcePropertySource source = new ResourcePropertySource(
                    "mainApplicationProperties",
                    new FileSystemResource("src/main/resources/application.properties")
            );
            environment.getPropertySources().addLast(source);
            ConfigurationPropertySources.attach(environment);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
    }

    @Configuration
    @EnableConfigurationProperties(SuApiConfig.class)
    static class TestConfig {

        @Bean
        ChannelE2EFlagProbe channelE2EFlagProbe() {
            return new ChannelE2EFlagProbe();
        }
    }

    static class ChannelE2EFlagProbe {
        @Value("${channel.e2e.local-enabled:false}")
        private boolean localEnabled;

        boolean isLocalEnabled() {
            return localEnabled;
        }
    }
}
