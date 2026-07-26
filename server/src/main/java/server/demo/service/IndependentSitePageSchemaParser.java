package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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
            throw new IllegalArgumentException("AI 页面配置为空");
        }
        if (output.length() > maxLength) {
            throw new IllegalArgumentException("AI 页面配置超过长度限制");
        }

        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI 页面配置不包含 JSON 对象");
        }
        String json = trimmed.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("AI 页面配置必须是 JSON 对象");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("AI 页面配置不是有效 JSON", e);
        }
    }
}
