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
                .withPropertyValues("CHANNEL_E2E_LOCAL_ENABLED=true")
                .run(context -> {
                    SuApiConfig suApiConfig = context.getBean(SuApiConfig.class);
                    ChannelE2EFlagProbe probe = context.getBean(ChannelE2EFlagProbe.class);

                    assertEquals("true", context.getEnvironment().getProperty("channel.e2e.local-enabled"));
                    assertEquals("true", context.getEnvironment().getProperty("su.api.local-mock-enabled"));
                    assertTrue(suApiConfig.isLocalMockEnabled());
                    assertTrue(probe.isLocalEnabled());
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
