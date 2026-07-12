package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.entity.Reservation;
import server.demo.entity.SuMessageThread;
import server.demo.repository.ReservationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationBookingKeyResolver {

    private final ReservationRepository reservationRepository;

    public ReservationBookingKeyResolver(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public Reservation findFirstReservationForThread(Long storeId, SuMessageThread thread) {
        if (storeId == null || thread == null) {
            return null;
        }

        for (String lookupKey : buildThreadLookupCandidates(thread)) {
            List<Reservation> reservations = findReservationsByBookingKeyWithRoomType(storeId, lookupKey);
            if (!reservations.isEmpty()) {
                return reservations.get(0);
            }
        }

        return null;
    }

    /**
     * Resolves one page of inbox threads with one reservation query. The home inbox deliberately
     * uses exact normalized keys here; the legacy fuzzy fallback remains available to detail pages
     * through {@link #findFirstReservationForThread(Long, SuMessageThread)}.
     */
    public Map<Long, Reservation> findFirstReservationsForThreads(
            Long storeId,
            List<SuMessageThread> threads
    ) {
        if (storeId == null || threads == null || threads.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> candidatesByThreadId = new LinkedHashMap<>();
        Set<String> allCandidates = new LinkedHashSet<>();
        for (SuMessageThread thread : threads) {
            if (thread == null || thread.getId() == null) {
                continue;
            }
            List<String> candidates = buildThreadLookupCandidates(thread);
            candidatesByThreadId.put(thread.getId(), candidates);
            allCandidates.addAll(candidates);
        }
        if (allCandidates.isEmpty()) {
            return Map.of();
        }

        List<Reservation> reservations = reservationRepository.findByStoreIdAndAnyBookingKeyInWithRoomType(
                storeId,
                new ArrayList<>(allCandidates)
        );
        Map<Long, Reservation> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<String>> entry : candidatesByThreadId.entrySet()) {
            Reservation match = findFirstMatchingReservation(reservations, entry.getValue());
            if (match != null) {
                result.put(entry.getKey(), match);
            }
        }
        return result;
    }

    private Reservation findFirstMatchingReservation(
            List<Reservation> reservations,
            List<String> lookupCandidates
    ) {
        if (reservations == null || reservations.isEmpty() || lookupCandidates == null) {
            return null;
        }
        for (String lookupCandidate : lookupCandidates) {
            for (Reservation reservation : reservations) {
                if (matchesReservationBookingKey(reservation, lookupCandidate)) {
                    return reservation;
                }
            }
        }
        return null;
    }

    public List<Reservation> findReservationsByBookingKey(Long storeId, String bookingKey) {
        return findReservationsByBookingKey(
                storeId,
                bookingKey,
                reservationRepository::findByStoreIdAndExternalBookingKey,
                reservationRepository::findByStoreIdAndChannelOrderNumber,
                this::findByOrderNumberExact,
                reservationRepository::findByStoreIdAndOrderNumberContainingIgnoreCase
        );
    }

    public List<Reservation> findReservationsByBookingKeyWithRoomType(Long storeId, String bookingKey) {
        return findReservationsByBookingKey(
                storeId,
                bookingKey,
                reservationRepository::findByStoreIdAndExternalBookingKeyWithRoomType,
                reservationRepository::findByStoreIdAndChannelOrderNumberWithRoomType,
                reservationRepository::findByStoreIdAndOrderNumberWithRoomType,
                reservationRepository::findByStoreIdAndOrderNumberContainingWithRoomType
        );
    }

    public List<String> buildThreadLookupCandidates(SuMessageThread thread) {
        Set<String> candidates = new LinkedHashSet<>();
        if (thread == null) {
            return List.of();
        }
        addLookupCandidates(candidates, thread.getBookingId());
        addLookupCandidates(candidates, thread.getThreadKey());
        addLookupCandidates(candidates, thread.getThreadId());
        return new ArrayList<>(candidates);
    }

    public List<String> buildReservationLookupCandidates(Reservation reservation) {
        Set<String> candidates = new LinkedHashSet<>();
        if (reservation == null) {
            return List.of();
        }
        addLookupCandidates(candidates, reservation.getChannelOrderNumber());
        addLookupCandidates(candidates, reservation.getExternalBookingKey());
        addLookupCandidates(candidates, reservation.getOrderNumber());
        return new ArrayList<>(candidates);
    }

    public String resolvePrimaryBookingKey(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        String channelOrderNumber = normalizeBookingLookupValue(reservation.getChannelOrderNumber());
        if (channelOrderNumber != null) {
            return channelOrderNumber;
        }

        String externalBookingKey = normalizeBookingLookupValue(reservation.getExternalBookingKey());
        if (externalBookingKey != null) {
            return externalBookingKey;
        }

        String extractedFromOrder = extractBookingKeyFromOrderLikeValue(reservation.getOrderNumber());
        if (extractedFromOrder != null) {
            return extractedFromOrder;
        }

        return normalizeBookingLookupValue(reservation.getOrderNumber());
    }

    public boolean matchesReservationBookingKey(Reservation reservation, String bookingKey) {
        String normalizedBookingKey = normalizeBookingLookupValue(bookingKey);
        if (reservation == null || normalizedBookingKey == null) {
            return false;
        }

        Set<String> bookingKeyCandidates = new LinkedHashSet<>();
        addLookupCandidates(bookingKeyCandidates, normalizedBookingKey);

        for (String candidate : buildReservationLookupCandidates(reservation)) {
            if (bookingKeyCandidates.contains(candidate)) {
                return true;
            }
        }

        return false;
    }

    static String normalizeBookingLookupValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim();
        return normalized.isBlank() ? null : normalized;
    }

    static String extractBookingKeyFromOrderLikeValue(String rawValue) {
        String normalized = normalizeBookingLookupValue(rawValue);
        if (normalized == null) {
            return null;
        }

        if (normalized.regionMatches(true, 0, "SU", 0, 2)) {
            int dashIndex = normalized.indexOf('-');
            if (dashIndex >= 0 && dashIndex + 1 < normalized.length()) {
                String suffix = normalized.substring(dashIndex + 1);
                int end = suffix.length();
                int suffixUnderscore = suffix.indexOf('_');
                if (suffixUnderscore >= 0) {
                    end = Math.min(end, suffixUnderscore);
                }
                int suffixDash = suffix.indexOf('-');
                if (suffixDash >= 0) {
                    end = Math.min(end, suffixDash);
                }

                String extracted = suffix.substring(0, end).trim();
                if (!extracted.isBlank()) {
                    return extracted;
                }
            }
        }

        int underscoreIndex = normalized.indexOf('_');
        if (underscoreIndex > 0) {
            String prefix = normalized.substring(0, underscoreIndex).trim();
            return prefix.isBlank() ? null : prefix;
        }

        return null;
    }

    private List<Reservation> findReservationsByBookingKey(
            Long storeId,
            String bookingKey,
            ReservationListFinder externalBookingKeyFinder,
            ReservationListFinder channelOrderNumberFinder,
            ReservationListFinder orderNumberFinder,
            ReservationListFinder orderNumberContainingFinder
    ) {
        String normalizedBookingKey = normalizeBookingLookupValue(bookingKey);
        if (storeId == null || normalizedBookingKey == null) {
            return List.of();
        }

        Set<String> lookupCandidates = new LinkedHashSet<>();
        addLookupCandidates(lookupCandidates, normalizedBookingKey);

        List<Reservation> matches = new ArrayList<>();
        for (String candidate : lookupCandidates) {
            addDistinctReservations(matches, externalBookingKeyFinder.find(storeId, candidate));
            addDistinctReservations(matches, channelOrderNumberFinder.find(storeId, candidate));
            addDistinctReservations(matches, orderNumberFinder.find(storeId, candidate));
        }

        for (String candidate : lookupCandidates) {
            List<Reservation> fuzzyMatches = orderNumberContainingFinder.find(storeId, candidate);
            for (Reservation reservation : fuzzyMatches) {
                if (reservation != null
                        && matchesReservationByAnyCandidate(reservation, lookupCandidates)
                        && !containsReservation(matches, reservation)) {
                    matches.add(reservation);
                }
            }
        }

        return matches;
    }

    private List<Reservation> findByOrderNumberExact(Long storeId, String orderNumber) {
        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber).orElse(null);
        return reservation == null ? List.of() : List.of(reservation);
    }

    private static void addLookupCandidates(Set<String> candidates, String rawValue) {
        String normalized = normalizeBookingLookupValue(rawValue);
        if (normalized != null) {
            candidates.add(normalized);
        }

        String extracted = extractBookingKeyFromOrderLikeValue(rawValue);
        if (extracted != null) {
            candidates.add(extracted);
        }
    }

    private static boolean matchesReservationByAnyCandidate(Reservation reservation, Set<String> lookupCandidates) {
        if (reservation == null || lookupCandidates.isEmpty()) {
            return false;
        }

        Set<String> reservationCandidates = new LinkedHashSet<>();
        addLookupCandidates(reservationCandidates, reservation.getChannelOrderNumber());
        addLookupCandidates(reservationCandidates, reservation.getExternalBookingKey());
        addLookupCandidates(reservationCandidates, reservation.getOrderNumber());

        for (String candidate : reservationCandidates) {
            if (lookupCandidates.contains(candidate)) {
                return true;
            }
        }

        return false;
    }

    private static void addDistinctReservations(List<Reservation> target, List<Reservation> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        for (Reservation candidate : candidates) {
            if (candidate != null && !containsReservation(target, candidate)) {
                target.add(candidate);
            }
        }
    }

    private static boolean containsReservation(List<Reservation> reservations, Reservation candidate) {
        for (Reservation existing : reservations) {
            if (existing == candidate) {
                return true;
            }
            if (existing != null
                    && candidate != null
                    && existing.getId() != null
                    && existing.getId().equals(candidate.getId())) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface ReservationListFinder {
        List<Reservation> find(Long storeId, String bookingKey);
    }
}
