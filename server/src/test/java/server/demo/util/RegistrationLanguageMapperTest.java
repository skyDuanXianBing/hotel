package server.demo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import server.demo.enums.ResidenceType;
import server.demo.service.RegistrationTargetLanguage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationLanguageMapperTest {

    @ParameterizedTest
    @CsvSource({
            "Japan, ja, Japanese",
            "China, zh-CN, Simplified Chinese",
            "Taiwan, zh-TW, Traditional Chinese",
            "'Hong Kong', zh-TW, Traditional Chinese",
            "'South Korea', ko, Korean",
            "'United States', en, English",
            "Thailand, th, Thai",
            "Vietnam, vi, Vietnamese",
            "Indonesia, id, Indonesian",
            "France, fr, French",
            "Spain, es, Spanish",
            "Germany, de, German",
            "Italy, it, Italian",
            "Russia, ru, Russian",
            "'Saudi Arabia', ar, Arabic"
    })
    void resolve_shouldMapCommonNationalities(String nationality, String expectedCode, String expectedName) {
        RegistrationTargetLanguage result = RegistrationLanguageMapper.resolve(nationality, null, null);

        assertTrue(result.isResolved());
        assertEquals(expectedCode, result.getCode());
        assertEquals(expectedName, result.getName());
        assertEquals(nationality, result.getSource());
    }

    @Test
    void resolve_shouldUseCountryWhenNationalityIsUnresolved() {
        RegistrationTargetLanguage result = RegistrationLanguageMapper.resolve("Atlantis", "France", null);

        assertTrue(result.isResolved());
        assertEquals("fr", result.getCode());
        assertEquals("French", result.getName());
        assertEquals("France", result.getSource());
    }

    @Test
    void resolve_shouldUseJapanResidenceWhenTextIsUnresolved() {
        RegistrationTargetLanguage result =
                RegistrationLanguageMapper.resolve("Atlantis", "Unknownland", ResidenceType.JAPAN);

        assertTrue(result.isResolved());
        assertEquals("ja", result.getCode());
        assertEquals("Japanese", result.getName());
        assertEquals("residenceType", result.getSource());
    }

    @Test
    void resolve_shouldFallbackToEnglishWhenNoSignalMatches() {
        RegistrationTargetLanguage result = RegistrationLanguageMapper.resolve("Atlantis", "Unknownland", null);

        assertFalse(result.isResolved());
        assertEquals("en", result.getCode());
        assertEquals("English", result.getName());
        assertEquals("UNRESOLVED_GUEST_LANGUAGE", result.getFallbackReason());
    }
}
