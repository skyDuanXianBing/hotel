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
import java.time.format.DateTimeParseException;
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
            MultipartFile airbnbFile,
            MultipartFile bookingFile,
            ManagedOperationDtos.RunRequest request) {
        YearMonth settlementMonth = validateRunRequest(request);
        ManagedOperationSettingsService.SettingsSnapshot snapshot = settingsService.requireSnapshot(storeId);
        List<ManagedOperationImportRow> airbnbRows = importParser.parseAirbnb(airbnbFile);
        List<ManagedOperationImportRow> bookingRows = importParser.parseBooking(bookingFile);
        List<ManagedOperationImportRow> imports = new ArrayList<>(airbnbRows.size() + bookingRows.size());
        imports.addAll(bookingRows);
        imports.addAll(airbnbRows);

        List<Reservation> candidates = reservationRepository.findByStoreIdAndCheckOutMonthWithRoomTypeAndChannel(
                storeId, settlementMonth.atDay(1), settlementMonth.plusMonths(1).atDay(1));
        Map<String, List<Reservation>> byKey = indexCandidates(candidates);

        Map<String, Long> relevantKeyCounts = imports.stream()
                .filter(row -> YearMonth.from(row.checkOutDate()).equals(settlementMonth))
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
                snapshot.settings().getRegistrationFeeNet(), request.deductions());

        EnumMap<ManagedOperationDtos.LineStatus, Integer> counts = new EnumMap<>(ManagedOperationDtos.LineStatus.class);
        for (ManagedOperationDtos.LineStatus status : ManagedOperationDtos.LineStatus.values()) counts.put(status, 0);
        for (ManagedOperationDtos.PreviewLine line : lines) counts.merge(line.status(), 1, Integer::sum);
        List<String> blockingReasons = new ArrayList<>();
        if (counts.get(ManagedOperationDtos.LineStatus.UNMATCHED) > 0) {
            blockingReasons.add("存在未匹配或关键资料冲突的本月订单");
        }
        if (counts.get(ManagedOperationDtos.LineStatus.AMBIGUOUS) > 0) {
            blockingReasons.add("存在重复预约号、歧义匹配或同一本地订单重复计入");
        }
        if (summary.includedReservationCount() == 0) {
            blockingReasons.add("没有可纳入结算的订单");
        }
        if (summary.settlementSubtotal().signum() < 0) {
            blockingReasons.add("精算小计为负数，不能生成汇款单据");
        }
        if (summary.finalTransfer().signum() < 0) {
            blockingReasons.add("最终汇款金额为负数，不能生成汇款单据");
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
        if (!YearMonth.from(row.checkOutDate()).equals(month)) {
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.PERIOD_EXCLUDED,
                    "退房日期不属于结算月份");
        }
        if (relevantKeyCounts.getOrDefault(row.platform() + ":" + row.bookingKey(), 0L) > 1) {
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.AMBIGUOUS,
                    "上传文件内预约号重复");
        }
        List<Reservation> exact = byKey.getOrDefault(row.bookingKey(), List.of()).stream()
                .filter(reservation -> isPlatform(reservation.getChannel(), row.platform()))
                .toList();
        if (exact.isEmpty()) {
            String reason = byKey.containsKey(row.bookingKey()) ? "本地订单渠道与上传平台不一致" : "未找到预约号完全一致的本地订单";
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.UNMATCHED, reason);
        }
        if (exact.size() > 1) {
            return excluded(row, null, "", cleaningFeeNet, ManagedOperationDtos.LineStatus.AMBIGUOUS,
                    "预约号匹配到多个本地订单");
        }
        Reservation reservation = exact.get(0);
        if (reservation.getCurrencyCode() != null && !reservation.getCurrencyCode().isBlank()
                && !"JPY".equalsIgnoreCase(reservation.getCurrencyCode().strip())) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.UNMATCHED, "本地订单币种不是 JPY");
        }
        if (!row.checkInDate().equals(reservation.getCheckInDate()) || !row.checkOutDate().equals(reservation.getCheckOutDate())) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.UNMATCHED, "上传日期与本地订单日期不一致");
        }
        if (!normalizeGuest(row.guestName()).equals(normalizeGuest(reservation.getGuestName()))) {
            warnings.add("上传客人姓名与本地订单不一致");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            return excluded(row, reservation.getId(), roomNumber(reservation), cleaningFeeNet,
                    ManagedOperationDtos.LineStatus.CANCELLED, "本地订单已取消或未到店", warnings);
        }

        String resolvedRoomNumber;
        if (reservation.getRoom() != null) {
            resolvedRoomNumber = reservation.getRoom().getRoomNumber();
            if (!selectedRoomIds.containsKey(reservation.getRoom().getId())) {
                return excluded(row, reservation.getId(), resolvedRoomNumber, cleaningFeeNet,
                        ManagedOperationDtos.LineStatus.ROOM_EXCLUDED, "订单不属于所选代运营房间", warnings);
            }
        } else {
            String otaRoomNumber = normalizeRoomNumber(reservation.getOtaRoomNumber());
            List<Room> mapped = selectedByNumber.getOrDefault(otaRoomNumber, List.of());
            if (otaRoomNumber.isBlank()) {
                return excluded(row, reservation.getId(), "", cleaningFeeNet,
                        ManagedOperationDtos.LineStatus.UNMATCHED, "本地订单尚未分配房间且没有 OTA 房间号", warnings);
            }
            if (mapped.size() != 1) {
                return excluded(row, reservation.getId(), reservation.getOtaRoomNumber(), cleaningFeeNet,
                        mapped.size() > 1 ? ManagedOperationDtos.LineStatus.AMBIGUOUS : ManagedOperationDtos.LineStatus.ROOM_EXCLUDED,
                        mapped.size() > 1 ? "OTA 房间号无法唯一映射" : "OTA 房间号不属于所选代运营房间", warnings);
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
                warnings.add("同一本地订单在上传数据中出现多次");
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
        if (request == null || request.settlementMonth() == null) {
            throw new ManagedOperationValidationException("请选择结算月份");
        }
        try {
            YearMonth month = YearMonth.parse(request.settlementMonth());
            if (request.deductions() != null && request.deductions().size() > 100) {
                throw new ManagedOperationValidationException("其他扣款项目不能超过 100 项");
            }
            if (request.note() != null && request.note().length() > 1000) {
                throw new ManagedOperationValidationException("备注不能超过 1000 字符");
            }
            for (String value : List.of(
                    request.invoiceNumber() == null ? "" : request.invoiceNumber(),
                    request.receiptNumber() == null ? "" : request.receiptNumber())) {
                if (value.length() > 100) throw new ManagedOperationValidationException("单据编号不能超过 100 字符");
            }
            return month;
        } catch (DateTimeParseException ex) {
            throw new ManagedOperationValidationException("结算月份格式必须为 YYYY-MM");
        }
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
