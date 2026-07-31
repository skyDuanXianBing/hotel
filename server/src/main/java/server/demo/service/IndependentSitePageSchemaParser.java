package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import server.demo.i18n.ApiMessages;
@Service
public class IndependentSitePageSchemaParser {

    private final ObjectMapper objectMapper;

    public IndependentSitePageSchemaParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(String output) {
        return parse(output, 12_000);
    }

    /**
     * 带上限参数的解析：BLOCKS 管线沿用 12_000，CANVAS 节点树更大，用 200_000。
     * 截取首个 { 至末个 } 的逻辑两格式共用。
     */
    public JsonNode parse(String output, int maxLength) {
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.e96e077abc75"));
        }
        if (output.length() > maxLength) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.9e638c4ee6d3"));
        }

        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.c33f6c124a7a"));
        }
        String json = trimmed.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.e7eca5b0b012"));
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.df362517ef00"), e);
        }
    }
}
