package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuestLanguageDetectorTest {

    @Test
    void detectLanguageName_shouldDetectCjkAndOtherDistinctiveScripts() {
        assertEquals("Japanese", GuestLanguageDetector.detectLanguageName("チェックイン時間は？"));
        assertEquals("Korean", GuestLanguageDetector.detectLanguageName("체크인 시간이 어떻게 되나요?"));
        assertEquals("Simplified Chinese", GuestLanguageDetector.detectLanguageName("请问几点可以入住？"));
        assertEquals("Thai", GuestLanguageDetector.detectLanguageName("สามารถเช็คอินก่อนได้ไหม"));
        assertEquals("Russian", GuestLanguageDetector.detectLanguageName("Можно ли заселиться раньше?"));
        assertEquals("Arabic", GuestLanguageDetector.detectLanguageName("هل يمكنني تسجيل الوصول مبكرًا؟"));
    }

    @Test
    void detectLanguageName_shouldReturnNullForLatinScriptOrBlank() {
        assertNull(GuestLanguageDetector.detectLanguageName("Can I check in early?"));
        assertNull(GuestLanguageDetector.detectLanguageName("Puis-je arriver plus tôt ?"));
        assertNull(GuestLanguageDetector.detectLanguageName("   "));
        assertNull(GuestLanguageDetector.detectLanguageName(null));
    }
}
