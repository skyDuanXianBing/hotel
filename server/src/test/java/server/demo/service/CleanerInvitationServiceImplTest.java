package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.CleanerRegistrationDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleanerInvitation;
import server.demo.entity.User;
import server.demo.repository.CleanerInvitationRepository;
import server.demo.repository.CleanerRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.impl.CleanerInvitationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanerInvitationServiceImplTest {

    @Test
    void registerCleaner_shouldReactivateExistingInactiveCleanerInSameStore() {
        CleanerInvitationRepository invitationRepository = Mockito.mock(CleanerInvitationRepository.class);
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        CleanerIdentityService cleanerIdentityService = Mockito.mock(CleanerIdentityService.class);

        CleanerInvitationServiceImpl service = new CleanerInvitationServiceImpl();
        ReflectionTestUtils.setField(service, "invitationRepository", invitationRepository);
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "emailService", emailService);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "cleanerIdentityService", cleanerIdentityService);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:8091");

        CleanerInvitation invitation = new CleanerInvitation();
        invitation.setId(1L);
        invitation.setToken("token-123");
        invitation.setEmail("cleaner@example.com");
        invitation.setName("Cleaner");
        invitation.setUserId(7L);
        invitation.setStoreId(26L);
        invitation.setStatus("pending");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));

        Cleaner inactiveCleaner = new Cleaner();
        inactiveCleaner.setId(5L);
        inactiveCleaner.setUserId(88L);
        inactiveCleaner.setStoreId(26L);
        inactiveCleaner.setEmail("cleaner@example.com");
        inactiveCleaner.setName("Old Name");
        inactiveCleaner.setPassword("old-password");
        inactiveCleaner.setIsActive(false);

        User reusedUser = new User();
        reusedUser.setId(88L);
        reusedUser.setPassword("encoded-password");

        CleanerRegistrationDTO registrationDTO = new CleanerRegistrationDTO();
        registrationDTO.setToken("token-123");
        registrationDTO.setEmail("cleaner@example.com");
        registrationDTO.setName("Cleaner New");
        registrationDTO.setPassword("plain-password");

        when(invitationRepository.findByToken("token-123")).thenReturn(Optional.of(invitation));
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(cleanerRepository.findByStoreIdAndEmailIgnoreCase(26L, "cleaner@example.com"))
                .thenReturn(List.of(inactiveCleaner));
        when(cleanerIdentityService.createOrReuseCleanerUserAccount(
                "cleaner@example.com",
                "Cleaner New",
                "encoded-password",
                26L,
                7L
        )).thenReturn(reusedUser);
        when(cleanerRepository.save(any(Cleaner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.save(any(CleanerInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cleaner cleaner = service.registerCleaner(registrationDTO);

        assertEquals(5L, cleaner.getId());
        assertEquals(88L, cleaner.getUserId());
        assertEquals("Cleaner New", cleaner.getName());
        assertEquals("encoded-password", cleaner.getPassword());
        assertEquals(Boolean.TRUE, cleaner.getIsActive());
        verify(passwordEncoder).encode("plain-password");
        verify(cleanerIdentityService).createOrReuseCleanerUserAccount(
                "cleaner@example.com",
                "Cleaner New",
                "encoded-password",
                26L,
                7L
        );
        verify(cleanerRepository).save(inactiveCleaner);
        assertEquals("accepted", invitation.getStatus());
    }
}
