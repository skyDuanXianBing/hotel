package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.entity.SuMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SuMessagingAiPromptBuilder {
    private static final int MAX_RECENT_MESSAGES = 12;
    private static final int MAX_SIMILAR_EXAMPLES = 3;
    private static final int MAX_REUSABLE_FACT_MATCHES = 3;
    private static final int MAX_PROMPT_LENGTH = 7000;

    private final SuMessagingAiRedactor redactor;
    private final MessageKnowledgeTopicService topicService;

    public SuMessagingAiPromptBuilder(
            SuMessagingAiRedactor redactor,
            MessageKnowledgeTopicService topicService
    ) {
        this.redactor = redactor;
        this.topicService = topicService;
    }

    public String buildPrompt(
            SuMessagingThreadContext context,
            List<SuMessage> storedRecentMessages,
            List<SuMessagingAiReplyDraftRequest.RecentMessage> requestRecentMessages,
            String latestGuestMessage,
            MessageKnowledgeSearchResult searchResult,
            String language
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a hotel guest messaging assistant.\n");
        prompt.append("Write only the reply draft that hotel staff can edit before sending.\n");
        prompt.append("Do not say you are AI. Do not invent policies, prices, access codes, or unavailable facts.\n");
        prompt.append("Use the same language as the latest guest message unless a target language is provided.\n");
        prompt.append("Return plain text only. No markdown, no JSON, no explanation.\n\n");

        appendLatestGuestMessage(prompt, latestGuestMessage);
        appendReusablePolicyFacts(prompt, searchResult);
        appendSimilarExamples(prompt, searchResult);
        appendRecentMessages(prompt, storedRecentMessages, requestRecentMessages);
        appendContext(prompt, context, language);
        prompt.append("Draft reply:");

        String result = prompt.toString();
        if (result.length() > MAX_PROMPT_LENGTH) {
            return result.substring(0, MAX_PROMPT_LENGTH);
        }
        return result;
    }

    private void appendLatestGuestMessage(StringBuilder prompt, String latestGuestMessage) {
        prompt.append("Latest guest message:\n");
        prompt.append(redactor.redact(latestGuestMessage)).append("\n\n");
    }

    private void appendReusablePolicyFacts(StringBuilder prompt, MessageKnowledgeSearchResult searchResult) {
        prompt.append("Reusable policy facts:\n");
        prompt.append("- Use these facts when they directly answer the latest guest message.\n");
        prompt.append("- Do not ask staff to confirm a fact already listed here. Ignore facts that are not relevant.\n");

        List<String> facts = collectReusableFacts(searchResult);
        if (facts.isEmpty()) {
            prompt.append("- No reusable policy facts found.\n\n");
            return;
        }

        for (String fact : facts) {
            prompt.append("- ").append(redactor.redact(fact)).append("\n");
        }
        prompt.append("\n");
    }

    private void appendContext(StringBuilder prompt, SuMessagingThreadContext context, String language) {
        prompt.append("Current thread context:\n");
        if (language != null && !language.isBlank()) {
            prompt.append("- Target language: ").append(redactor.redact(language)).append("\n");
        }
        if (context == null) {
            prompt.append("- Store-scoped message thread, no room context resolved.\n\n");
            return;
        }
        appendLine(prompt, "Channel", context.getChannelName());
        appendLine(prompt, "Guest name", context.getGuestName());
        appendLine(prompt, "Booking key", context.getBookingKey());
        appendLine(prompt, "Check-in date", context.getCheckInDate() == null ? null : context.getCheckInDate().toString());
        appendLine(prompt, "Check-out date", context.getCheckOutDate() == null ? null : context.getCheckOutDate().toString());
        appendLine(prompt, "Room number", context.getRoomNumber());
        appendLine(prompt, "Room type", context.getRoomTypeName());
        prompt.append("- Context match status: ").append(context.getMatchStatus()).append("\n\n");
    }

    private void appendRecentMessages(
            StringBuilder prompt,
            List<SuMessage> storedRecentMessages,
            List<SuMessagingAiReplyDraftRequest.RecentMessage> requestRecentMessages
    ) {
        prompt.append("Recent conversation:\n");
        int appended = appendStoredMessages(prompt, storedRecentMessages);
        if (appended == 0) {
            appended = appendRequestMessages(prompt, requestRecentMessages);
        }
        if (appended == 0) {
            prompt.append("- No recent messages available.\n");
        }
        prompt.append("\n");
    }

    private int appendStoredMessages(StringBuilder prompt, List<SuMessage> storedRecentMessages) {
        if (storedRecentMessages == null || storedRecentMessages.isEmpty()) {
            return 0;
        }

        int appended = 0;
        for (int index = storedRecentMessages.size() - 1; index >= 0; index--) {
            if (appended >= MAX_RECENT_MESSAGES) {
                return appended;
            }
            SuMessage message = storedRecentMessages.get(index);
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            prompt.append("- ")
                    .append(message.getSenderType())
                    .append(": ")
                    .append(redactor.redact(message.getContent()))
                    .append("\n");
            appended++;
        }
        return appended;
    }

    private int appendRequestMessages(
            StringBuilder prompt,
            List<SuMessagingAiReplyDraftRequest.RecentMessage> requestRecentMessages
    ) {
        if (requestRecentMessages == null || requestRecentMessages.isEmpty()) {
            return 0;
        }

        int start = Math.max(0, requestRecentMessages.size() - MAX_RECENT_MESSAGES);
        int appended = 0;
        for (int index = start; index < requestRecentMessages.size(); index++) {
            SuMessagingAiReplyDraftRequest.RecentMessage message = requestRecentMessages.get(index);
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String direction = message.getDirection() == null ? "UNKNOWN" : message.getDirection();
            prompt.append("- ")
                    .append(direction)
                    .append(": ")
                    .append(redactor.redact(message.getContent()))
                    .append("\n");
            appended++;
        }
        return appended;
    }

    private void appendSimilarExamples(StringBuilder prompt, MessageKnowledgeSearchResult searchResult) {
        prompt.append("Similar historical resolved Q&A:\n");
        if (searchResult == null || searchResult.getMatches().isEmpty()) {
            prompt.append("- No similar historical answer was found.\n");
            prompt.append("\n");
            return;
        }

        int count = Math.min(MAX_SIMILAR_EXAMPLES, searchResult.getMatches().size());
        for (int index = 0; index < count; index++) {
            MessageKnowledgeMatch match = searchResult.getMatches().get(index);
            MessageKnowledgeEntry entry = match.getEntry();
            prompt.append("- Example ")
                    .append(index + 1)
                    .append(" (")
                    .append(match.getScopeLevel())
                    .append(", score ")
                    .append(String.format("%.2f", match.getScore()))
                    .append(")\n");
            prompt.append("  Guest: ").append(redactor.redact(entry.getQuestion())).append("\n");
            prompt.append("  Staff: ").append(redactor.redact(entry.getAnswer())).append("\n");
        }
        prompt.append("\n");
    }

    private List<String> collectReusableFacts(MessageKnowledgeSearchResult searchResult) {
        List<String> facts = new ArrayList<>();
        if (searchResult == null || searchResult.getMatches().isEmpty()) {
            return facts;
        }

        Set<String> seenFacts = new LinkedHashSet<>();
        int count = Math.min(MAX_REUSABLE_FACT_MATCHES, searchResult.getMatches().size());
        for (int index = 0; index < count; index++) {
            MessageKnowledgeMatch match = searchResult.getMatches().get(index);
            if (match == null || match.getEntry() == null) {
                continue;
            }
            List<String> matchFacts = match.getReusableFacts();
            if (matchFacts == null || matchFacts.isEmpty()) {
                matchFacts = topicService.extractReusableFacts(match.getEntry().getAnswer());
            }
            for (String fact : matchFacts) {
                if (fact == null || fact.isBlank()) {
                    continue;
                }
                if (seenFacts.add(fact)) {
                    facts.add(fact);
                }
            }
        }
        return facts;
    }

    private void appendLine(StringBuilder prompt, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        prompt.append("- ").append(label).append(": ").append(redactor.redact(value)).append("\n");
    }
}
