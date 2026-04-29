package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.auth.CleanerLoginRequest;
import server.demo.dto.auth.CleanerLoginResponse;
import server.demo.entity.Cleaner;
import server.demo.repository.CleanerRepository;
import server.demo.util.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanerAuthServiceTest {

    @Test
    void loginByPassword_shouldGenerateTokenWithRepairedUserId() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);
        CleanerIdentityService cleanerIdentityService = Mockito.mock(CleanerIdentityService.class);

        CleanerAuthService service = new CleanerAuthService();
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(service, "cleanerIdentityService", cleanerIdentityService);

        Cleaner cleaner = new Cleaner();
        cleaner.setId(3L);
        cleaner.setUserId(88L);
        cleaner.setEmail("cleaner@example.com");
        cleaner.setPassword("encoded-password");
        cleaner.setIsActive(true);

        CleanerLoginRequest request = new CleanerLoginRequest();
        request.setEmail("cleaner@example.com");
        request.setPassword("plain-password");

        Mockito.when(cleanerRepository.findByEmail("cleaner@example.com")).thenReturn(Optional.of(cleaner));
        Mockito.when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        Mockito.when(cleanerIdentityService.ensureCleanerIdentity(cleaner)).thenReturn(cleaner);
        Mockito.when(jwtUtil.generateToken(88L, "cleaner@example.com")).thenReturn("token-123");

        CleanerLoginResponse response = service.loginByPassword(request);

        assertEquals("token-123", response.getToken());
        assertEquals(88L, response.getCleaner().getUserId());
        Mockito.verify(jwtUtil).generateToken(88L, "cleaner@example.com");
    }
}
