package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import server.demo.dto.registration.RegistrationLinkInboxItemDTO;
import server.demo.entity.RegistrationLinkInboxItem;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationLinkInboxRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RegistrationLinkInboxService {

    private final RegistrationLinkInboxRepository inboxRepository;
    private final RegistrationLinkService registrationLinkService;
    private final ReservationBookingKeyResolver reservationBookingKeyResolver;
    private final String serverBaseUrl;

    public RegistrationLinkInboxService(
            RegistrationLinkInboxRepository inboxRepository,
            RegistrationLinkService registrationLinkService,
            ReservationBookingKeyResolver reservationBookingKeyResolver,
            @Value("${server.base-url}") String serverBaseUrl
    ) {
        this.inboxRepository = inboxRepository;
        this.registrationLinkService = registrationLinkService;
        this.reservationBookingKeyResolver = reservationBookingKeyResolver;
        this.serverBaseUrl = serverBaseUrl;
    }

    public void recordIfAbsent(Long storeId,
                               String bookingKey,
                               String guestName,
                               LocalDate checkInDate,
                               LocalDate checkOutDate,
                               Integer roomCount) {
        if (storeId == null || bookingKey == null || bookingKey.isBlank()) {
            return;
        }

        String key = bookingKey.trim();
        Optional<RegistrationLinkInboxItem> existing = inboxRepository.findByStoreIdAndBookingKey(storeId, key);
        if (existing.isPresent()) {
            return;
        }

        RegistrationLinkInboxItem item = new RegistrationLinkInboxItem();
        item.setStoreId(storeId);
        item.setBookingKey(key);
        item.setGuestName(guestName);
        item.setCheckInDate(checkInDate);
        item.setCheckOutDate(checkOutDate);
        item.setRoomCount(roomCount != null && roomCount > 0 ? roomCount : 1);
        item.setLinkUrl(registrationLinkService.buildPublicBookingLink(serverBaseUrl, storeId, key));

        try {
            inboxRepository.save(item);
        } catch (DataIntegrityViolationException ignore) {
            // unique constraint: someone else already inserted
        }
    }

    public List<RegistrationLinkInboxItemDTO> listTop200(Long storeId, ReservationStatus reservationStatus) {
        List<RegistrationLinkInboxItem> items = inboxRepository.findTop200ByStoreIdOrderByCreatedAtDesc(storeId);
        List<RegistrationLinkInboxItemDTO> dtos = new ArrayList<>();
        URI baseUri = parseBaseUri(serverBaseUrl);
        for (RegistrationLinkInboxItem it : items) {
            ReservationStatus resolvedStatus = resolveReservationStatus(storeId, it.getBookingKey());
            if (reservationStatus != null && resolvedStatus != reservationStatus) {
                continue;
            }

            RegistrationLinkInboxItemDTO dto = new RegistrationLinkInboxItemDTO();
            dto.setId(it.getId());
            dto.setBookingKey(it.getBookingKey());
            dto.setLinkUrl(resolveLinkUrl(baseUri, it.getLinkUrl(), storeId, it.getBookingKey()));
            dto.setGuestName(it.getGuestName());
            dto.setCheckInDate(it.getCheckInDate());
            dto.setCheckOutDate(it.getCheckOutDate());
            dto.setRoomCount(it.getRoomCount());
            dto.setReservationStatus(resolvedStatus);
            dto.setCreatedAt(it.getCreatedAt());
            dtos.add(dto);
        }
        return dtos;
    }

    private ReservationStatus resolveReservationStatus(Long storeId, String bookingKey) {
        if (storeId == null || bookingKey == null || bookingKey.isBlank()) {
            return null;
        }

        List<Reservation> reservations = reservationBookingKeyResolver.findReservationsByBookingKey(storeId, bookingKey);
        if (reservations.isEmpty()) {
            return null;
        }

        return reservations.stream()
                .map(Reservation::getStatus)
                .filter(status -> status != null)
                .max(Comparator.comparingInt(this::statusPriority))
                .orElse(null);
    }

    private int statusPriority(ReservationStatus status) {
        return switch (status) {
            case CANCELLED -> 100;
            case NO_SHOW -> 90;
            case CHECKED_IN -> 80;
            case CHECKED_OUT -> 70;
            case CONFIRMED -> 60;
            case REQUESTED -> 50;
        };
    }

    private URI parseBaseUri(String base) {
        if (base == null || base.isBlank()) {
            return null;
        }
        try {
            String trimmed = base.trim();
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String resolveLinkUrl(URI baseUri, String storedUrl, Long storeId, String bookingKey) {
        // Prefer preserving existing token/path, but upgrade scheme/host to current server.base-url.
        if (baseUri != null && storedUrl != null && !storedUrl.isBlank()) {
            try {
                URI u = URI.create(storedUrl.trim());
                if (u.getPath() != null && !u.getPath().isBlank()) {
                    URI rebuilt = new URI(
                            baseUri.getScheme(),
                            baseUri.getUserInfo(),
                            baseUri.getHost(),
                            baseUri.getPort(),
                            u.getPath(),
                            u.getQuery(),
                            u.getFragment()
                    );
                    return rebuilt.toString();
                }
            } catch (Exception ignore) {
                // Fall through.
            }
        }

        // Fallback: regenerate a link (token changes, but remains valid).
        return registrationLinkService.buildPublicBookingLink(serverBaseUrl, storeId, bookingKey);
    }
}
