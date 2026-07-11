package server.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartLockConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SmartLockConfig.class);

    @Test
    void switchBotPasscodeWrite_shouldDefaultToDisabled() {
        contextRunner.run(context -> {
            assertTrue(context.isRunning());
            assertFalse(context.getBean(SmartLockConfig.class).isSwitchBotPasscodeWriteEnabled());
        });
    }

    @Test
    void switchBotPasscodeWrite_shouldRespectExplicitFalseAndTrue() {
        contextRunner
                .withPropertyValues("smart-lock.switchbot.passcode-write-enabled=false")
                .run(context -> assertFalse(
                        context.getBean(SmartLockConfig.class).isSwitchBotPasscodeWriteEnabled()
                ));
        contextRunner
                .withPropertyValues("smart-lock.switchbot.passcode-write-enabled=true")
                .run(context -> assertTrue(
                        context.getBean(SmartLockConfig.class).isSwitchBotPasscodeWriteEnabled()
                ));
    }

    @Test
    void switchBotPasscodeWrite_shouldFailClosedForInvalidValue() {
        contextRunner
                .withPropertyValues("smart-lock.switchbot.passcode-write-enabled=not-a-boolean")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                });
    }
}
