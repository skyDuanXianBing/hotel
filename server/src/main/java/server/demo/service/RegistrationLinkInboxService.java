package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import server.demo.dto.registration.RegistrationLinkInboxItemDTO;
import server.demo.entity.RegistrationLinkInboxItem;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationLinkInboxRepository;
import server.demo.repository.ReservationRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistrationLinkInboxService {

    private static final int DEFAULT_ROOM_COUNT = 1;

    private final RegistrationLinkInboxRepository inboxRepository;
    private final RegistrationLinkService registrationLinkService;
    private final ReservationBookingKeyResolver reservationBookingKeyResolver;
    private final ReservationRepository reservationRepository;
    private final String frontendBaseUrl;

    public RegistrationLinkInboxService(
            RegistrationLinkInboxRepository inboxRepository,
            RegistrationLinkService registrationLinkService,
            ReservationBookingKeyResolver reservationBookingKeyResolver,
            ReservationRepository reservationRepository,
            @Value("${app.frontend.url}") String frontendBaseUrl
    ) {
        this.inboxRepository = inboxRepository;
        this.registrationLinkService = registrationLinkService;
        this.reservationBookingKeyResolver = reservationBookingKeyResolver;
        this.reservationRepository = reservationRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public record BackfillResult(
            int scannedCount,
            int eligibleCount,
            int createdCount,
            int skippedMissingBookingKey
    ) {}

    public boolean recordIfAbsent(Long storeId,
                                  Reservation reservation,
                                  Integer roomCount) {
        if (reservation == null) {
            return false;
        }

        String bookingKey = reservationBookingKeyResolver.resolvePrimaryBookingKey(reservation);
        return recordIfAbsent(
                storeId,
                bookingKey,
                reservation.getGuestName(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                roomCount
        );
    }

    public boolean recordIfAbsent(Long storeId,
                                  String bookingKey,
                                  String guestName,
                                  LocalDate checkInDate,
                                  LocalDate checkOutDate,
                                  Integer roomCount) {
        if (storeId == null || bookingKey == null || bookingKey.isBlank()) {
            return false;
        }

        String key = bookingKey.trim();
        Optional<RegistrationLinkInboxItem> existing = inboxRepository.findByStoreIdAndBookingKey(storeId, key);
        if (existing.isPresent()) {
            return false;
        }

        RegistrationLinkInboxItem item = new RegistrationLinkInboxItem();
        item.setStoreId(storeId);
        item.setBookingKey(key);
        item.setGuestName(guestName);
        item.setCheckInDate(checkInDate);
        item.setCheckOutDate(checkOutDate);
        item.setRoomCount(roomCount != null && roomCount > 0 ? roomCount : DEFAULT_ROOM_COUNT);
        item.setLinkUrl(registrationLinkService.buildPublicBookingLink(frontendBaseUrl, storeId, key));

        try {
            inboxRepository.save(item);
            return true;
        } catch (DataIntegrityViolationException ignore) {
            // unique constraint: someone else already inserted
            return false;
        }
    }

    public BackfillResult backfillMissingForStore(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        List<Reservation> reservations = reservationRepository.findByStoreId(storeId);
        if (reservations == null) {
            reservations = List.of();
        }
        Map<String, InboxBackfillCandidate> candidatesByBookingKey = new LinkedHashMap<>();
        int skippedMissingBookingKey = 0;

        for (Reservation reservation : reservations) {
            if (reservation == null) {
                skippedMissingBookingKey++;
                continue;
            }

            String bookingKey = reservationBookingKeyResolver.resolvePrimaryBookingKey(reservation);
            if (bookingKey == null || bookingKey.isBlank()) {
                skippedMissingBookingKey++;
                continue;
            }

            String normalizedBookingKey = bookingKey.trim();
            InboxBackfillCandidate candidate = candidatesByBookingKey.get(normalizedBookingKey);
            if (candidate == null) {
                candidate = new InboxBackfillCandidate(normalizedBookingKey);
                candidatesByBookingKey.put(normalizedBookingKey, candidate);
            }
            candidate.merge(reservation);
        }

        int createdCount = 0;
        for (InboxBackfillCandidate candidate : candidatesByBookingKey.values()) {
            boolean created = recordIfAbsent(
                    storeId,
                    candidate.bookingKey,
                    candidate.guestName,
                    candidate.checkInDate,
                    candidate.checkOutDate,
                    candidate.roomCount
            );
            if (created) {
                createdCount++;
            }
        }

        return new BackfillResult(
                reservations.size(),
                candidatesByBookingKey.size(),
                createdCount,
                skippedMissingBookingKey
        );
    }

    public List<RegistrationLinkInboxItemDTO> listTop200(Long storeId, ReservationStatus reservationStatus) {
        List<RegistrationLinkInboxItem> items = inboxRepository.findTop200ByStoreIdOrderByCreatedAtDesc(storeId);
        List<RegistrationLinkInboxItemDTO> dtos = new ArrayList<>();
        URI baseUri = parseBaseUri(frontendBaseUrl);
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
        // Prefer preserving existing token/path, but upgrade scheme/host to current frontend URL.
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
        return registrationLinkService.buildPublicBookingLink(frontendBaseUrl, storeId, bookingKey);
    }

    private static final class InboxBackfillCandidate {
        private final String bookingKey;
        private String guestName;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private int roomCount;

        private InboxBackfillCandidate(String bookingKey) {
            this.bookingKey = bookingKey;
        }

        private void merge(Reservation reservation) {
            if (reservation == null) {
                return;
            }

            if (guestName == null || guestName.isBlank()) {
                guestName = reservation.getGuestName();
            }

            LocalDate reservationCheckIn = reservation.getCheckInDate();
            if (reservationCheckIn != null && (checkInDate == null || reservationCheckIn.isBefore(checkInDate))) {
                checkInDate = reservationCheckIn;
            }

            LocalDate reservationCheckOut = reservation.getCheckOutDate();
            if (reservationCheckOut != null && (checkOutDate == null || reservationCheckOut.isAfter(checkOutDate))) {
                checkOutDate = reservationCheckOut;
            }

            roomCount++;
        }
    }
}
