package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.dto.FacilityDTO;
import server.demo.dto.LocalizedContentDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonFieldUtilsTest {

    @Test
    void shouldRoundTripStringList() {
        String json = JsonFieldUtils.writeStringList(List.of(" a ", "b", "a"));
        assertEquals(List.of("a", "b"), JsonFieldUtils.readStringList(json));
    }

    @Test
    void shouldRoundTripLocalizedContent() {
        LocalizedContentDTO content = new LocalizedContentDTO();
        content.setName("中文名");
        content.setDescription("中文描述");

        Map<String, LocalizedContentDTO> values = new LinkedHashMap<>();
        values.put("zh-CN", content);

        String json = JsonFieldUtils.writeLocalizedContentMap(values);
        Map<String, LocalizedContentDTO> parsed = JsonFieldUtils.readLocalizedContentMap(json);

        assertEquals("中文名", parsed.get("zh-CN").getName());
        assertEquals("中文描述", parsed.get("zh-CN").getDescription());
    }

    @Test
    void shouldRoundTripFacilities() {
        List<FacilityDTO> values = List.of(
                new FacilityDTO("Amenities", "WiFi"),
                new FacilityDTO("Amenities", "WiFi"),
                new FacilityDTO("Bathroom", "Hair Dryer")
        );

        String json = JsonFieldUtils.writeFacilityList(values);
        List<FacilityDTO> parsed = JsonFieldUtils.readFacilityList(json);

        assertEquals(2, parsed.size());
        assertEquals("Amenities", parsed.get(0).getGroup());
        assertEquals("WiFi", parsed.get(0).getName());
        assertEquals("Bathroom", parsed.get(1).getGroup());
        assertEquals("Hair Dryer", parsed.get(1).getName());
    }
}
