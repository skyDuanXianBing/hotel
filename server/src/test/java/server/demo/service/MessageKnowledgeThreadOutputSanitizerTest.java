package server.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKnowledgeThreadOutputSanitizerTest {

    @Test
    void sanitize_shouldAllowReusableUrlTemplateWithPlaceholders() {
        MessageKnowledgeThreadOutputSanitizer sanitizer = new MessageKnowledgeThreadOutputSanitizer();

        MessageKnowledgeThreadSanitizedText result = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "Guests can complete pre-check-in at https://hotel.example.com/checkin/{bookingId}.",
                new SuMessagingThreadContext()
        ));

        assertFalse(result.rejected());
        assertTrue(result.generalized());
    }

    @Test
    void sanitize_shouldRejectPiiAndPrivateUrlValues() {
        MessageKnowledgeThreadOutputSanitizer sanitizer = new MessageKnowledgeThreadOutputSanitizer();

        MessageKnowledgeThreadSanitizedText phone = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "Please call +81 90-1234-5678 for this guest.",
                new SuMessagingThreadContext()
        ));
        MessageKnowledgeThreadSanitizedText email = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "The guest email is guest@example.com.",
                new SuMessagingThreadContext()
        ));
        MessageKnowledgeThreadSanitizedText order = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "Booking number ABCD1234 must be shown at reception.",
                new SuMessagingThreadContext()
        ));
        MessageKnowledgeThreadSanitizedText code = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "The door code is 1234 for this stay.",
                new SuMessagingThreadContext()
        ));
        MessageKnowledgeThreadSanitizedText privateUrl = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "Use https://hotel.example.com/checkin?token=abc123 to register.",
                new SuMessagingThreadContext()
        ));

        assertTrue(phone.rejected());
        assertTrue(email.rejected());
        assertTrue(order.rejected());
        assertTrue(code.rejected());
        assertTrue(privateUrl.rejected());
    }

    @Test
    void sanitize_shouldRejectKnownGuestNameAndBookingKeyFromContext() {
        MessageKnowledgeThreadOutputSanitizer sanitizer = new MessageKnowledgeThreadOutputSanitizer();
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setGuestName("Jane Doe");
        context.setBookingKey("BK-2026-999");

        MessageKnowledgeThreadSanitizedText result = sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(
                "Jane Doe can use booking BK-2026-999 at the desk.",
                context
        ));

        assertTrue(result.rejected());
        assertTrue(result.reasonCodes().contains("booking_key"));
        assertTrue(result.reasonCodes().contains("guest_name"));
    }
}
