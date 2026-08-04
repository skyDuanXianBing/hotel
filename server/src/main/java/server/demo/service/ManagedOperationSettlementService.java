package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.Channel;
import server.demo.entity.ManagedOperationSettings;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.enums.ReservationStatus;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ReservationRepository;
import server.demo.service.managedoperation.ManagedOperationImportParser;
import server.demo.service.managedoperation.ManagedOperationImportRow;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import server.demo.i18n.ApiMessages;
@Service
public class ManagedOperationSettlementService {
    private final ManagedOperationSettingsService settingsService;
    private final ManagedOperationImportParser importParser;
    private final ReservationRepository reservationRepository;
    private final ManagedOperationCalculationService calculationService;

    public ManagedOperationSettlementService(
            ManagedOperationSettingsService settingsService,
            ManagedOperationImportParser importParser,
            ReservationRepository reservationRepository,
            ManagedOperationCalculationService calculationService) {
        this.settingsService = settingsService;
        this.importParser = importParser;
        this.reservationRepository = reservationRepository;
        this.calculationService = calculationService;
    }

    @Transactional(readOnly = true)
    public CalculationResult calculate(
            Long storeId,
            Long settingsId,
            MultipartFile airbnbFile,
            MultipartFile bookingFile,
            ManagedOperationDtos.RunRequest request) {
        YearMonth settlementMonth = validateRunRequest(request);
        ManagedOperationSettingsService.SettingsSnapshot snapshot = settingsService.requireSnapshot(storeId, settingsId);
        List<ManagedOperationImportRow> airbnbRows = importParser.parseAirbnb(airbnbFile);
        List<ManagedOperationImportRow> bookingRows = importParser.parseBooking(bookingFile);
        List<ManagedOperationImportRow> imports = new ArrayList<>(airbnbRows.size() + bookingRows.size());
        imports.addAll(bookingRows);
        imports.addAll(airbnbRows);

        // 入金在结算月之后到账的订单可能属于前几个月退房，候选窗口向前扩展 3 个月
        List<Reservation> candidates = reservationRepository.findByStoreIdAndCheckOutMonthWithRoomTypeAndChannel(
                storeId, settlementMonth.minusMonths(3).atDay(1), settlementMonth.plusMonths(1).atDay(1));
        Map<String, List<Reservation>> byKey = indexCandidates(candidates);

        Map<String, Long> relevantKeyCounts = imports.stream()
                .filter(row -> isRelevantForMonth(row, settlementMonth))
                .collect(Collectors.groupingBy(
                        row -> row.platform() + ":" + row.bookingKey(), LinkedHashMap::new, Collectors.counting()));
        Map<Long, Integer> selectedRoomIndexes = new HashMap<>();
        Map<String, List<Room>> selectedByNumber = new HashMap<>();
        for (Room room : snapshot.rooms()) {
            selectedRoomIndexes.put(room.getId(), 1);
            selectedByNumber.computeIfAbsent(normalizeRoomNumber(room.getRoomNumber()), unused -> new ArrayList<>()).add(room);
        }

        BigDecimal cleaningFeeNet = calculationService.cleaningFeeNet(
                snapshot.settings().getCleaningFeeGross(), snapshot.settings().getTaxRate());
        List<EvaluatedLine> evaluated = new ArrayList<>();
        for (ManagedOperationImportRow row : imports) {
            evaluated.add(evaluate(row, settlementMonth, relevantKeyCounts, byKey, selectedRoomIndexes,
                    selectedByNumber, cleaningFeeNet, snapshot.settings().getManagementFeeRate()));
        }
        preventDuplicateLocalReservation(evaluated);

        List<ManagedOperationDtos.PreviewLine> lines = evaluated.stream().map(EvaluatedLine::line).toList();
        List<ManagedOperationCalculationService.RowAmounts> includedAmounts = evaluated.stream()
                .filter(line -> line.line().status() == ManagedOperationDtos.LineStatus.INCLUDED)
                .map(EvaluatedLine::amounts).toList();
        ManagedOperationDtos.PreviewSummary summary = calculationService.summarize(
                includedAmounts, snapshot.rooms().size(), snapshot.settings().getCleaningFeeGross(),
                snapshot.settings().getManagementFeeRate(), snapshot.settings().getTaxRate(),
                snapshot.settings().getRegistrationFeeNet(), request.fees());

        EnumMap<ManagedOperationDtos.LineStatus, Integer> counts = new EnumMap<>(ManagedOperationDtos.LineStatus.class);
        for (ManagedOperationDtos.LineStatus status : ManagedOperationDtos.LineStatus.values()) counts.put(status, 0);
        for (ManagedOperationDtos.PreviewLine line : lines) counts.merge(line.status(), 1, Integer::sum);
        List<String> blockingReasons = new ArrayList<>();
        if (counts.get(ManagedOperationDtos.LineStatus.UNMATCHED) > 0) {
            blockingReasons.add(ApiMessages.get("api.t.3d3475643542"));
        }
        if (counts.get(ManagedOperationDtos.LineStatus.AMBIGUOUS) > 0) {
            blockingReasons.add(ApiMessages.get("api.t.a7688fff0fbb"));
        }
        if (summary.includedReservationCount() == 0) {
            blockingReasons.add(ApiMessages.get("api.t.19057091b1bf"));
        }
        if (summary.settlementSubtotal().signum() < 0) {
            blockingReasons.add(ApiMessages.get("api.t.d11a76a58370"));
        }
        if (summary.finalTransfer().signum() < 0) {
            blockingReasons.add(ApiMessages.get("api.t.f3f0ffd04610"));
        }
        ManagedOperationDtos.PreviewResponse response = new ManagedOperationDtos.PreviewResponse(
                lines,
                new ManagedOperationDtos.PreviewStats(airbnbRows.size(), bookingRows.size(), counts),
                summary,
                blockingReasons.isEmpty(),
                List.copyOf(blockingReasons));
        return new CalculationResult(response, snapshot.settings(), snapshot.rooms(), request, settlementMonth);
    }

    private EvaluatedLine evaluate(
            ManagedOperationImportRow row,
            YearMonth month,
            Map<String, Long> relevantKeyCounts,
            Map<String, List<Reservation>> byKey,
            Map<Long, Integer> selectedRoomIds,
            Map<String, List<Room>> selectedByNumber,
            BigDecimal cleaningFeeNet,
            BigDecimal managementFeeRate) {
        List<String> warnings = new ArrayList<>();
        if (!isRelevantForMonth(row, month)) {
            String reason = row.platform() == ManagedOperationImportRow.Platform.AIRBNB
                    && row.payoutDate() == null
                    ? ApiMessages.get("api.t.9cfb658147bd")
                    : ApiMessages.get("api.t.247e1414e189");
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.PERIOD_EXCLUDED,
                    reason);
        }
        if (relevantKeyCounts.getOrDefault(row.platform() + ":" + row.bookingKey(), 0L) > 1) {
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.AMBIGUOUS,
                    ApiMessages.get("api.t.af8d9ddef234"));
        }
        List<Reservation> exact = byKey.getOrDefault(row.bookingKey(), List.of()).stream()
                .filter(reservation -> isPlatform(reservation.getChannel(), row.platform()))
                .toList();
        if (exact.isEmpty()) {
            String reason = byKey.containsKey(row.bookingKey()) ? ApiMessages.get("api.t.ac1bd52fa75d") : ApiMessages.get("api.t.428abacf8652");
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.UNMATCHED, reason);
        }
        if (exact.size() > 1) {
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.AMBIGUOUS,
                    ApiMessages.get("api.t.6d365e9e64cd"));
        }
        Reservation reservation = exact.get(0);
        if (reservation.getCurrencyCode() != null && !reservation.getCurrencyCode().isBlank()
                && !"JPY".equalsIgnoreCase(reservation.getCurrencyCode().strip())) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.UNMATCHED, ApiMessages.get("api.t.988b5117a39c"));
        }
        if (!row.checkInDate().equals(reservation.getCheckInDate()) || !row.checkOutDate().equals(reservation.getCheckOutDate())) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.UNMATCHED, ApiMessages.get("api.t.fae321668c12"));
        }
        if (!normalizeGuest(row.guestName()).equals(normalizeGuest(reservation.getGuestName()))) {
            warnings.add(ApiMessages.get("api.t.d2c8ec2c3467"));
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.CANCELLED, ApiMessages.get("api.t.2f3622cc37b7"), warnings);
        }

        String resolvedRoomNumber;
        if (reservation.getRoom() != null) {
            resolvedRoomNumber = reservation.getRoom().getRoomNumber();
            if (!selectedRoomIds.containsKey(reservation.getRoom().getId())) {
                return excluded(row, reservation.getId(), resolvedRoomNumber, cleaningFeeNet,
                        ManagedOperationDtos.LineStatus.ROOM_EXCLUDED, ApiMessages.get("api.t.de8020133742"), warnings);
            }
        } else {
            String otaRoomNumber = normalizeRoomNumber(reservation.getOtaRoomNumber());
            List<Room> mapped = selectedByNumber.getOrDefault(otaRoomNumber, List.of());
            if (otaRoomNumber.isBlank()) {
                return excluded(row, reservation.getId(), "", cleaningFeeNet,
                        ManagedOperationDtos.LineStatus.UNMATCHED, ApiMessages.get("api.t.14ce900772bc"), warnings);
            }
            if (mapped.size() != 1) {
                return excluded(row, reservation.getId(), reservation.getOtaRoomNumber(), cleaningFeeNet,
                        mapped.size() > 1 ? ManagedOperationDtos.LineStatus.AMBIGUOUS : ManagedOperationDtos.LineStatus.ROOM_EXCLUDED,
                        mapped.size() > 1 ? ApiMessages.get("api.t.aac343415e5e") : ApiMessages.get("api.t.e0223e385850"), warnings);
            }
            resolvedRoomNumber = mapped.get(0).getRoomNumber();
        }

        ManagedOperationCalculationService.RowAmounts amounts = calculationService.calculateRow(
                row, cleaningFeeNet, managementFeeRate);
        return new EvaluatedLine(toLine(row, resolvedRoomNumber, cleaningFeeNet,
                amounts, ManagedOperationDtos.LineStatus.INCLUDED, warnings), reservation.getId(), amounts);
    }

    private static void preventDuplicateLocalReservation(List<EvaluatedLine> evaluated) {
        Map<Long, Long> counts = evaluated.stream()
                .filter(item -> item.reservationId() != null && item.line().status() == ManagedOperationDtos.LineStatus.INCLUDED)
                .collect(Collectors.groupingBy(EvaluatedLine::reservationId, Collectors.counting()));
        for (int i = 0; i < evaluated.size(); i++) {
            EvaluatedLine item = evaluated.get(i);
            if (item.reservationId() != null && counts.getOrDefault(item.reservationId(), 0L) > 1) {
                List<String> warnings = new ArrayList<>(item.line().warnings());
                warnings.add(ApiMessages.get("api.t.bc312853a20e"));
                evaluated.set(i, new EvaluatedLine(withStatus(item.line(), ManagedOperationDtos.LineStatus.AMBIGUOUS, warnings),
                        item.reservationId(), null));
            }
        }
    }

    private static Map<String, List<Reservation>> indexCandidates(List<Reservation> candidates) {
        Map<String, List<Reservation>> result = new HashMap<>();
        for (Reservation reservation : candidates) {
            Set<String> keys = new HashSet<>();
            keys.add(ManagedOperationImportParser.normalizeBookingKey(reservation.getChannelOrderNumber()));
            keys.add(ManagedOperationImportParser.normalizeBookingKey(reservation.getExternalBookingKey()));
            keys.add(ManagedOperationImportParser.normalizeBookingKey(reservation.getOrderNumber()));
            for (String key : keys) {
                if (!key.isBlank()) result.computeIfAbsent(key, unused -> new ArrayList<>()).add(reservation);
            }
        }
        return result;
    }

    static boolean isPlatform(Channel channel, ManagedOperationImportRow.Platform platform) {
        if (channel == null || channel.getCode() == null || platform == null) return false;
        String code = channel.getCode().strip().toUpperCase(Locale.ROOT);
        return switch (platform) {
            case AIRBNB -> "AIRBNB".equals(code);
            case BOOKING -> "BOOKING".equals(code)
                    || "BOOKING.COM".equals(code)
                    || "BOOKING_COM".equals(code);
        };
    }

    private static EvaluatedLine excluded(ManagedOperationImportRow row, Long reservationId, String roomNumber,
                                          BigDecimal cleaningFeeNet, ManagedOperationDtos.LineStatus status, String warning) {
        return excluded(row, reservationId, roomNumber, cleaningFeeNet, status, warning, new ArrayList<>());
    }

    private static EvaluatedLine excluded(ManagedOperationImportRow row, Long reservationId, String roomNumber,
                                          BigDecimal cleaningFeeNet, ManagedOperationDtos.LineStatus status,
                                          String warning, List<String> existingWarnings) {
        List<String> warnings = new ArrayList<>(existingWarnings);
        warnings.add(warning);
        return new EvaluatedLine(toLine(row, roomNumber, cleaningFeeNet, null, status, warnings), reservationId, null);
    }

    private static ManagedOperationDtos.PreviewLine toLine(
            ManagedOperationImportRow row, String roomNumber, BigDecimal cleaningFeeNet,
            ManagedOperationCalculationService.RowAmounts amounts,
            ManagedOperationDtos.LineStatus status, List<String> warnings) {
        return new ManagedOperationDtos.PreviewLine(
                row.platform().name(), row.sourceRowNumber(), row.bookingKey(), row.checkInDate(), row.checkOutDate(),
                row.guestName(), roomNumber, row.currency(), row.grossSales(), row.otaServiceFee(), row.payoutFee(),
                cleaningFeeNet,
                amounts == null ? null : amounts.receivedAmount(),
                amounts == null ? null : amounts.managementFee(),
                amounts == null ? null : amounts.scheduledTransfer(),
                row.payoutDate(), row.payoutReference(), status, List.copyOf(warnings));
    }

    private static ManagedOperationDtos.PreviewLine withStatus(
            ManagedOperationDtos.PreviewLine line, ManagedOperationDtos.LineStatus status, List<String> warnings) {
        return new ManagedOperationDtos.PreviewLine(
                line.platform(), line.sourceRowNumber(), line.bookingKey(), line.checkInDate(), line.checkOutDate(),
                line.guestName(), line.roomNumber(), line.currency(), line.grossSales(), line.otaServiceFee(),
                line.payoutFee(), line.cleaningFeeNet(), null, null, null, line.payoutDate(), line.payoutReference(),
                status, List.copyOf(warnings));
    }

    private static YearMonth validateRunRequest(ManagedOperationDtos.RunRequest request) {
        if (request == null) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.7e604be81e55"));
        }
        YearMonth month = ManagedOperationRunFieldsValidator.requireMonth(request.settlementMonth());
        ManagedOperationRunFieldsValidator.validateFees(request.fees());
        ManagedOperationRunFieldsValidator.validateNote(request.note());
        ManagedOperationRunFieldsValidator.validateDocumentNumbers(request.invoiceNumber(), request.receiptNumber());
        return month;
    }

    /**
     * 结算归属判断：Airbnb 按入金予定日截取（用户会故意多导出行，系统只取入金日落在结算月的行）；
     * Booking 月结账单全部纳入，不再按入住/退房月过滤。
     */
    static boolean isRelevantForMonth(ManagedOperationImportRow row, YearMonth month) {
        if (row.platform() == ManagedOperationImportRow.Platform.BOOKING) {
            return true;
        }
        return row.payoutDate() != null && YearMonth.from(row.payoutDate()).equals(month);
    }

    private static String normalizeRoomNumber(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeGuest(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String roomNumber(Reservation reservation) {
        return reservation.getRoom() == null ? reservation.getOtaRoomNumber() : reservation.getRoom().getRoomNumber();
    }

    private record EvaluatedLine(
            ManagedOperationDtos.PreviewLine line,
            Long reservationId,
            ManagedOperationCalculationService.RowAmounts amounts) {}

    public record CalculationResult(
            ManagedOperationDtos.PreviewResponse preview,
            ManagedOperationSettings settings,
            List<Room> selectedRooms,
            ManagedOperationDtos.RunRequest request,
            YearMonth settlementMonth) {}
}
