package server.demo.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuReviewWebhookAuthConfigTest {

    @Test
    void remainsFailClosedUntilNonBlankAuthorizationIsConfigured() {
        SuReviewWebhookAuthConfig config = new SuReviewWebhookAuthConfig();

        assertFalse(config.isConfigured());
        assertFalse(config.matches(null));
        assertFalse(config.matches("anything"));

        config.setAuthorization(" ");

        assertFalse(config.isConfigured());
        assertFalse(config.matches(" "));
    }

    @Test
    void comparesTheCompleteAuthorizationValueWithoutAssumingAScheme() {
        SuReviewWebhookAuthConfig config = new SuReviewWebhookAuthConfig();
        config.setAuthorization("Su-Signature keyId=test,signature=abc");

        assertTrue(config.matches("Su-Signature keyId=test,signature=abc"));
        assertFalse(config.matches("su-signature keyId=test,signature=abc"));
        assertFalse(config.matches("Su-Signature keyId=test,signature=abd"));
        assertFalse(config.matches(" Su-Signature keyId=test,signature=abc"));
    }
}
