package server.demo.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SuMessagingAiTextService {
    private static final int MIN_LATIN_TOKEN_LENGTH = 3;
    private static final int MAX_TOKENS = 24;
    private static final int CJK_BIGRAM_SIZE = 2;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Set<String> LATIN_STOP_WORDS = Set.of(
            "can",
            "could",
            "would",
            "please",
            "thank",
            "thanks",
            "you",
            "your",
            "our",
            "the",
            "and",
            "for",
            "with",
            "have",
            "has",
            "had",
            "get",
            "got",
            "let",
            "know",
            "tell",
            "ask",
            "need",
            "want",
            "will",
            "may",
            "are",
            "was",
            "were",
            "that",
            "this",
            "there",
            "where",
            "when",
            "what",
            "how",
            "who",
            "which",
            "out"
    );

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}\\p{P}]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public List<String> tokenize(String text) {
        String normalized = normalize(text);
        Set<String> tokens = new LinkedHashSet<>();
        addLatinTokens(tokens, normalized);
        addCjkBigrams(tokens, normalized);
        return new ArrayList<>(tokens);
    }

    public String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] bytes = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void addLatinTokens(Set<String> tokens, String normalized) {
        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            if (tokens.size() >= MAX_TOKENS) {
                return;
            }
            if (part.length() >= MIN_LATIN_TOKEN_LENGTH
                    && containsLetterOrDigit(part)
                    && !LATIN_STOP_WORDS.contains(part)) {
                tokens.add(part);
            }
        }
    }

    private static void addCjkBigrams(Set<String> tokens, String normalized) {
        List<Character> cjkCharacters = new ArrayList<>();
        for (int index = 0; index < normalized.length(); index++) {
            char value = normalized.charAt(index);
            if (isCjk(value)) {
                cjkCharacters.add(value);
            }
        }

        for (int index = 0; index + CJK_BIGRAM_SIZE <= cjkCharacters.size(); index++) {
            if (tokens.size() >= MAX_TOKENS) {
                return;
            }
            String token = "" + cjkCharacters.get(index) + cjkCharacters.get(index + 1);
            tokens.add(token);
        }
    }

    private static boolean containsLetterOrDigit(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isLetterOrDigit(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }
}
