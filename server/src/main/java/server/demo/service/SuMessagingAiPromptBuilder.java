package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingAiReplyDraftRequest;
import server.demo.entity.SuMessage;
import server.demo.enums.SuMessagingSenderType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SuMessagingAiPromptBuilder {
    private static final int MAX_RECENT_MESSAGES = 30;
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
            String latestGuestTurn,
            MessageKnowledgeSearchResult searchResult,
            String language
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a hotel guest messaging assistant.\n");
        prompt.append("Write only the reply draft that hotel staff can edit before sending.\n");
        prompt.append("Do not say you are AI. Do not invent policies, prices, access codes, or unavailable facts.\n");
        prompt.append("Reply to every item in the final consecutive guest turn that needs a response.\n");
        prompt.append("If the final sentence is only thanks or greeting, still check earlier guest messages in the same turn for unanswered requests.\n");
        prompt.append("Do not answer only the final courtesy message when the same guest turn contains another request.\n");
        prompt.append("Use the same language as the final guest turn unless a target language is provided.\n");
        prompt.append("Return plain text only. No markdown, no JSON, no explanation.\n\n");

        appendLatestGuestTurn(prompt, latestGuestTurn);
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

    private void appendLatestGuestTurn(StringBuilder prompt, String latestGuestTurn) {
        prompt.append("Last guest turn to reply to:\n");
        prompt.append(redactor.redact(latestGuestTurn)).append("\n\n");
    }

    private void appendReusablePolicyFacts(StringBuilder prompt, MessageKnowledgeSearchResult searchResult) {
        List<String> facts = collectReusableFacts(searchResult);
        if (facts.isEmpty()) {
            return;
        }

        prompt.append("Reusable policy facts:\n");
        prompt.append("- Use these facts when they directly answer the final guest turn.\n");
        prompt.append("- Do not ask staff to confirm a fact already listed here. Ignore facts that are not relevant.\n");
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
                    .append(formatStoredRole(message.getSenderType()))
                    .append("：")
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
            prompt.append("- ")
                    .append(formatRequestRole(message.getDirection()))
                    .append("：")
                    .append(redactor.redact(message.getContent()))
                    .append("\n");
            appended++;
        }
        return appended;
    }

    private void appendSimilarExamples(StringBuilder prompt, MessageKnowledgeSearchResult searchResult) {
        if (searchResult == null || searchResult.getMatches().isEmpty()) {
            return;
        }

        prompt.append("Similar historical resolved Q&A:\n");
        int count = Math.min(MAX_SIMILAR_EXAMPLES, searchResult.getMatches().size());
        for (int index = 0; index < count; index++) {
            MessageKnowledgeMatch match = searchResult.getMatches().get(index);
            MessageKnowledgeCandidate candidate = match.getCandidate();
            prompt.append("- Example ")
                    .append(index + 1)
                    .append(" (")
                    .append(match.getScopeLevel())
                    .append(", score ")
                    .append(String.format("%.2f", match.getScore()))
                    .append(")\n");
            prompt.append("  Guest: ").append(redactor.redact(candidate.getQuestion())).append("\n");
            prompt.append("  Staff: ").append(redactor.redact(candidate.getAnswer())).append("\n");
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
            if (match == null || match.getCandidate() == null) {
                continue;
            }
            List<String> matchFacts = match.getReusableFacts();
            if (matchFacts == null || matchFacts.isEmpty()) {
                matchFacts = topicService.extractReusableFacts(match.getCandidate().getAnswer());
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

    private static String formatStoredRole(SuMessagingSenderType senderType) {
        if (senderType == SuMessagingSenderType.GUEST) {
            return "客户";
        }
        if (senderType == SuMessagingSenderType.STAFF) {
            return "员工";
        }
        return "未知";
    }

    private static String formatRequestRole(String direction) {
        if (direction == null || direction.isBlank()) {
            return "未知";
        }
        String normalized = direction.trim();
        if ("GUEST".equalsIgnoreCase(normalized)
                || "CUSTOMER".equalsIgnoreCase(normalized)
                || "USER".equalsIgnoreCase(normalized)) {
            return "客户";
        }
        if ("STAFF".equalsIgnoreCase(normalized)
                || "HOST".equalsIgnoreCase(normalized)
                || "EMPLOYEE".equalsIgnoreCase(normalized)) {
            return "员工";
        }
        if ("SYSTEM".equalsIgnoreCase(normalized)) {
            return "系统";
        }
        return "未知";
    }
}
