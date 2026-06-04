package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.entity.RegistrationGuest;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.util.RegistrationLanguageMapper;

import java.util.List;

@Service
public class RegistrationLanguageResolver {
    private static final String FALLBACK_REASON_NO_GUEST = "NO_REGISTRATION_GUEST";

    private final RegistrationGuestRepository registrationGuestRepository;

    public RegistrationLanguageResolver(RegistrationGuestRepository registrationGuestRepository) {
        this.registrationGuestRepository = registrationGuestRepository;
    }

    public RegistrationTargetLanguage resolveTargetLanguage(Long formId) {
        if (formId == null) {
            return RegistrationTargetLanguage.defaultEnglish(FALLBACK_REASON_NO_GUEST);
        }

        List<RegistrationGuest> guests = registrationGuestRepository.findByFormIdOrderBySortOrderAsc(formId);
        if (guests == null || guests.isEmpty()) {
            return RegistrationTargetLanguage.defaultEnglish(FALLBACK_REASON_NO_GUEST);
        }

        RegistrationTargetLanguage fallback = null;
        for (RegistrationGuest guest : guests) {
            if (guest == null) {
                continue;
            }
            RegistrationTargetLanguage language = RegistrationLanguageMapper.resolve(
                    guest.getNationality(),
                    guest.getCountry(),
                    guest.getResidenceType()
            );
            if (language.isResolved()) {
                return language;
            }
            if (fallback == null) {
                fallback = language;
            }
        }

        if (fallback != null) {
            return fallback;
        }
        return RegistrationTargetLanguage.defaultEnglish(FALLBACK_REASON_NO_GUEST);
    }
}
