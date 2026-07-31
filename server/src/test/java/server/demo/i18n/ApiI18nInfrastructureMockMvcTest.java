package server.demo.i18n;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.controller.advice.ApiValidationExceptionHandler;
import server.demo.dto.ApiResponse;
import server.demo.dto.auth.LoginByCodeRequest;
import server.demo.interceptor.AdminAuthInterceptor;
import server.demo.interceptor.JwtInterceptor;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Infrastructure i18n: interceptor 401 envelopes, default success message, and main-API validation → ApiResponse.
 */
class ApiI18nInfrastructureMockMvcTest {

    private MockMvc jwtMockMvc;
    private MockMvc adminMockMvc;
    private MockMvc apiMockMvc;

    @BeforeEach
    void setUp() {
        TestApiMessages.install();

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "i18n-infra-test-secret-key-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600_000L);

        RedisUtil redisUtil = Mockito.mock(RedisUtil.class);
        Mockito.when(redisUtil.isTokenBlacklisted(anyString())).thenReturn(false);

        JwtInterceptor jwtInterceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(jwtInterceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(jwtInterceptor, "redisUtil", redisUtil);

        AdminAuthInterceptor adminAuthInterceptor = new AdminAuthInterceptor();
        ReflectionTestUtils.setField(adminAuthInterceptor, "jwtUtil", jwtUtil);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(TestApiMessages.messageSource());
        validator.afterPropertiesSet();

        jwtMockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setLocaleResolver(new AppLocaleResolver())
                .addInterceptors(jwtInterceptor)
                .build();

        adminMockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setLocaleResolver(new AppLocaleResolver())
                .addInterceptors(adminAuthInterceptor)
                .build();

        apiMockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setLocaleResolver(new AppLocaleResolver())
                .setControllerAdvice(new ApiValidationExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void jwtUnauthorized_withXAppLocaleEn_returnsEnglishMessage() throws Exception {
        jwtMockMvc.perform(get("/api/v1/i18n-probe/success")
                        .header(AppLocale.HEADER_APP_LOCALE, "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication token not provided"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void jwtUnauthorized_withXAppLocaleJa_returnsJapaneseMessage() throws Exception {
        jwtMockMvc.perform(get("/api/v1/i18n-probe/success")
                        .header(AppLocale.HEADER_APP_LOCALE, "ja"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("認証トークンが提供されていません"));
    }

    @Test
    void adminUnauthorized_withXAppLocaleEn_returnsEnglishMessage() throws Exception {
        adminMockMvc.perform(get("/api/v1/i18n-probe/success")
                        .header(AppLocale.HEADER_APP_LOCALE, "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication token not provided"));
    }

    @Test
    void apiResponseSuccess_defaultMessageLocalizesByLocaleContext() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en"));
        try {
            ApiResponse<Void> en = ApiResponse.success(null);
            assertEquals("Operation successful", en.getMessage());
            assertEquals("api.common.success", en.getMessageKey());
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }

        LocaleContextHolder.setLocale(Locale.forLanguageTag("ja"));
        try {
            ApiResponse<Void> ja = ApiResponse.success(null);
            assertEquals("操作が成功しました", ja.getMessage());
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void apiResponseSuccess_viaMockMvc_withXAppLocaleEn() throws Exception {
        apiMockMvc.perform(get("/api/v1/i18n-probe/success")
                        .header(AppLocale.HEADER_APP_LOCALE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Operation successful"))
                .andExpect(jsonPath("$.messageKey").value("api.common.success"));
    }

    @Test
    void validFailure_withXAppLocaleEn_returnsApiResponseWithLocalizedMessage() throws Exception {
        // Only omit email so the asserted field error is deterministic (code alone is valid).
        String payload = "{\"verificationCode\":\"123456\"}";
        apiMockMvc.perform(post("/api/v1/i18n-probe/login-code")
                        .header(AppLocale.HEADER_APP_LOCALE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is required"));

        String body = apiMockMvc.perform(post("/api/v1/i18n-probe/login-code")
                        .header(AppLocale.HEADER_APP_LOCALE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(body.contains("邮箱"));
        assertFalse(body.contains("api.t."));
    }

    @RestController
    @RequestMapping("/api/v1/i18n-probe")
    static class ProbeController {

        @GetMapping("/success")
        public ApiResponse<Void> success() {
            return ApiResponse.success(null);
        }

        @PostMapping("/login-code")
        public ApiResponse<Void> loginCode(@Valid @RequestBody LoginByCodeRequest request) {
            return ApiResponse.success(null);
        }
    }
}
