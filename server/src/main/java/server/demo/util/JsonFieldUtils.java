package server.demo.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.demo.dto.FacilityDTO;
import server.demo.dto.LocalizedContentDTO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 文本字段与对象之间的转换工具。
 */
public final class JsonFieldUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonFieldUtils() {
    }

    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析字符串列表失败", e);
        }
    }

    public static String writeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化字符串列表失败", e);
        }
    }

    public static Map<String, LocalizedContentDTO> readLocalizedContentMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<LinkedHashMap<String, LocalizedContentDTO>>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("解析多语言内容失败", e);
        }
    }

    public static String writeLocalizedContentMap(Map<String, LocalizedContentDTO> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Map<String, LocalizedContentDTO> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, LocalizedContentDTO> entry : values.entrySet()) {
            String locale = entry.getKey();
            LocalizedContentDTO content = entry.getValue();
            if (locale == null || locale.isBlank() || content == null) {
                continue;
            }
            LocalizedContentDTO item = new LocalizedContentDTO();
            item.setName(content.getName() != null ? content.getName().trim() : null);
            item.setDescription(content.getDescription() != null ? content.getDescription().trim() : null);
            sanitized.put(locale.trim(), item);
        }
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化多语言内容失败", e);
        }
    }

    public static List<FacilityDTO> readFacilityList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<FacilityDTO>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析设施列表失败", e);
        }
    }

    public static String writeFacilityList(List<FacilityDTO> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Map<String, FacilityDTO> sanitizedMap = new LinkedHashMap<>();
        for (FacilityDTO value : values) {
            if (value == null || value.getName() == null || value.getName().isBlank()) {
                continue;
            }
            String group = value.getGroup() != null && !value.getGroup().isBlank()
                    ? value.getGroup().trim()
                    : "Common";
            String name = value.getName().trim();
            sanitizedMap.putIfAbsent(group + "|" + name, new FacilityDTO(group, name));
        }
        List<FacilityDTO> sanitized = List.copyOf(sanitizedMap.values());
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化设施列表失败", e);
        }
    }
}
