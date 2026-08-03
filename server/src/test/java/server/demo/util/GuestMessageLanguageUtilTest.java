package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.Reservation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestMessageLanguageUtilTest {

    @Test
    void japaneseLanguageCodes_areJapanese() {
        assertTrue(GuestMessageLanguageUtil.isJapanese("ja", null, null));
        assertTrue(GuestMessageLanguageUtil.isJapanese("JA", null, null));
        assertTrue(GuestMessageLanguageUtil.isJapanese("ja-JP", null, null));
        assertTrue(GuestMessageLanguageUtil.isJapanese("jpn", null, null));
        assertTrue(GuestMessageLanguageUtil.isJapanese("Japanese", null, null));
        assertTrue(GuestMessageLanguageUtil.isJapanese("日本語", null, null));
    }

    @Test
    void japaneseCountry_isJapanese() {
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, "JP", null));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, "jp", null));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, "JPN", null));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, "Japan", null));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, "日本", null));
    }

    @Test
    void japanesePhoneWithCountryCode_isJapanese() {
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, null, "+81-90-1234-5678"));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, null, "+819012345678"));
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, null, "0081-90-1234-5678"));
        // 多号码（逗号分隔）任一命中即可
        assertTrue(GuestMessageLanguageUtil.isJapanese(null, null, "+1-555-0100,+81-90-1234-5678"));
    }

    @Test
    void localPhoneWithoutCountryCode_isNotJapanese() {
        // 不带国家码的本地号码不猜测，避免误判
        assertFalse(GuestMessageLanguageUtil.isJapanese(null, null, "090-1234-5678"));
        assertFalse(GuestMessageLanguageUtil.isJapanese(null, null, "07012345678"));
        // 不带 + 的 81 开头也不认
        assertFalse(GuestMessageLanguageUtil.isJapanese(null, null, "81-90-1234-5678"));
    }

    @Test
    void nonJapaneseGuest_isNotJapanese() {
        assertFalse(GuestMessageLanguageUtil.isJapanese(null, null, null));
        assertFalse(GuestMessageLanguageUtil.isJapanese("", "", ""));
        assertFalse(GuestMessageLanguageUtil.isJapanese("en", "US", "+1-555-0100"));
        assertFalse(GuestMessageLanguageUtil.isJapanese("zh-CN", "CN", "+86-138-0000-0000"));
        assertFalse(GuestMessageLanguageUtil.isJapanese("ko", "KR", "+82-10-1234-5678"));
    }

    @Test
    void reservation_nullSafe() {
        assertFalse(GuestMessageLanguageUtil.isJapaneseGuest(null));

        Reservation reservation = new Reservation();
        assertFalse(GuestMessageLanguageUtil.isJapaneseGuest(reservation));

        reservation.setGuestPhone("+81-90-1234-5678");
        assertTrue(GuestMessageLanguageUtil.isJapaneseGuest(reservation));

        Reservation byCountry = new Reservation();
        byCountry.setGuestCountry("JP");
        assertTrue(GuestMessageLanguageUtil.isJapaneseGuest(byCountry));

        Reservation byLang = new Reservation();
        byLang.setGuestLanguage("ja");
        assertTrue(GuestMessageLanguageUtil.isJapaneseGuest(byLang));
    }

    @Test
    void languageWinsOverNonJapaneseCountry() {
        // 任一信号命中即判日本（语言/国家/电话是并列信号，非覆盖关系）
        assertTrue(GuestMessageLanguageUtil.isJapanese("ja", "US", "+1-555-0100"));
        assertTrue(GuestMessageLanguageUtil.isJapanese("en", "JP", "+1-555-0100"));
    }
}
