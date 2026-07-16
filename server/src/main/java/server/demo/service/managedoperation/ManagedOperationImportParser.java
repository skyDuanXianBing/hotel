package server.demo.service.managedoperation;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.service.ManagedOperationMoneyRules;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ManagedOperationImportParser {
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/uuuu"),
            DateTimeFormatter.ofPattern("uuuu/M/d"),
            DateTimeFormatter.ofPattern("uuuu年M月d日"));

    private final SafeSpreadsheetReader spreadsheetReader;

    public ManagedOperationImportParser(SafeSpreadsheetReader spreadsheetReader) {
        this.spreadsheetReader = spreadsheetReader;
    }

    public List<ManagedOperationImportRow> parseAirbnb(MultipartFile file) {
        SafeSpreadsheetReader.Table table = spreadsheetReader.read(file);
        HeaderMap h = new HeaderMap(table.headers());
        int type = h.required("種別", "タイプ");
        int key = h.required("確認コード", "確認番号");
        int checkIn = h.required("開始日", "チェックイン日");
        int checkOut = h.required("終了日", "チェックアウト日", "Checkout");
        int guest = h.required("ゲスト", "宿泊者氏名");
        int listing = h.optional("リスティング", "房源");
        int currency = h.required("通貨", "币种");
        int gross = h.required("総収入", "总收入");
        int service = h.required("サービス料", "Airbnbサービス料");
        int payoutDateColumn = h.optional("入金予定日", "支払予定日");
        int referenceColumn = h.optional("参照コード", "お支払い番号", "支払い番号");

        List<ManagedOperationImportRow> result = new ArrayList<>();
        LocalDate pendingPayoutDate = null;
        String pendingReference = "";
        int rowNumber = 1;
        for (List<String> row : table.rows()) {
            rowNumber++;
            String rowType = value(row, type);
            if (rowType.equalsIgnoreCase("Payout") || rowType.equals("支払い") || rowType.equals("入金")) {
                pendingPayoutDate = optionalDate(value(row, payoutDateColumn));
                pendingReference = safeText(value(row, referenceColumn), 200);
                continue;
            }
            if (!(rowType.equals("予約") || rowType.equalsIgnoreCase("reservation"))) {
                continue;
            }
            String currencyValue = requireJpy(value(row, currency), rowNumber);
            result.add(new ManagedOperationImportRow(
                    ManagedOperationImportRow.Platform.AIRBNB,
                    rowNumber,
                    bookingKey(value(row, key), rowNumber),
                    date(value(row, checkIn), "开始日", rowNumber),
                    date(value(row, checkOut), "结束日", rowNumber),
                    safeText(value(row, guest), 200),
                    safeText(value(row, listing), 500),
                    currencyValue,
                    money(value(row, gross), "总收入", rowNumber),
                    money(value(row, service), "服务费", rowNumber).abs(),
                    BigDecimal.ZERO,
                    pendingPayoutDate,
                    pendingReference));
            pendingPayoutDate = null;
            pendingReference = "";
        }
        if (result.isEmpty()) throw new ManagedOperationValidationException("Airbnb 文件没有预约数据");
        return result;
    }

    public List<ManagedOperationImportRow> parseBooking(MultipartFile file) {
        SafeSpreadsheetReader.Table table = spreadsheetReader.read(file);
        HeaderMap h = new HeaderMap(table.headers());
        int key = h.required("予約番号", "确认代码", "確認コード");
        int checkIn = h.required("チェックイン日", "開始日");
        int checkOut = h.required("Checkout", "チェックアウト日", "終了日", "退房日");
        int guest = h.required("宿泊者氏名", "ゲスト");
        int currency = h.required("通貨", "币种");
        int gross = h.required("金額", "売上金額", "销售额");
        int commission = h.required("コミッション", "OTAサービス料", "佣金");
        int payoutFee = h.required("決済サービスの手数料", "振込手数料", "支付服务费");
        int payoutDate = h.optional("お支払い日", "支払い日", "入金予定日");
        int payoutReference = h.optional("お支払い番号", "支払い番号", "参照コード");

        List<ManagedOperationImportRow> result = new ArrayList<>();
        int rowNumber = 1;
        for (List<String> row : table.rows()) {
            rowNumber++;
            if (row.stream().allMatch(String::isBlank)) continue;
            String currencyValue = requireJpy(value(row, currency), rowNumber);
            result.add(new ManagedOperationImportRow(
                    ManagedOperationImportRow.Platform.BOOKING,
                    rowNumber,
                    bookingKey(value(row, key), rowNumber),
                    date(value(row, checkIn), "入住日", rowNumber),
                    date(value(row, checkOut), "退房日", rowNumber),
                    safeText(value(row, guest), 200),
                    "",
                    currencyValue,
                    money(value(row, gross), "金额", rowNumber),
                    money(value(row, commission), "佣金", rowNumber).abs(),
                    money(value(row, payoutFee), "支付手续费", rowNumber).abs(),
                    optionalDate(value(row, payoutDate)),
                    safeText(value(row, payoutReference), 200)));
        }
        if (result.isEmpty()) throw new ManagedOperationValidationException("Booking 文件没有预约数据");
        return result;
    }

    public static String normalizeBookingKey(String raw) {
        String value = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFKC).strip();
        if (value.matches("[+-]?\\d+\\.0+")) {
            value = value.substring(0, value.indexOf('.'));
        }
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String bookingKey(String value, int row) {
        String normalized = normalizeBookingKey(value);
        if (normalized.isBlank() || normalized.length() > 120) {
            throw new ManagedOperationValidationException("第 " + row + " 行预约号不合法");
        }
        return normalized;
    }

    private static String requireJpy(String value, int row) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!"JPY".equals(normalized)) {
            throw new ManagedOperationValidationException("第 " + row + " 行币种不是 JPY，不能与日元固定费用混算");
        }
        return normalized;
    }

    private static BigDecimal money(String raw, String field, int row) {
        String value = raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFKC).strip();
        boolean parentheses = value.startsWith("(") && value.endsWith(")");
        value = value.replace(",", "").replace("¥", "").replace("￥", "").replace(" ", "");
        if (parentheses) value = "-" + value.substring(1, value.length() - 1);
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.abs().compareTo(new BigDecimal("1000000000")) > 0) throw new NumberFormatException();
            ManagedOperationMoneyRules.requireWholeYen(amount, "第 " + row + " 行" + field);
            return amount;
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (NumberFormatException ex) {
            throw new ManagedOperationValidationException("第 " + row + " 行" + field + "不是有效金额");
        }
    }

    private static LocalDate date(String raw, String field, int row) {
        LocalDate value = optionalDate(raw);
        if (value == null) throw new ManagedOperationValidationException("第 " + row + " 行" + field + "不是有效日期");
        return value;
    }

    private static LocalDate optionalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = Normalizer.normalize(raw.strip(), Normalizer.Form.NFKC);
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        // Some OTA exports store dates as General-formatted Excel serial numbers rather than date-formatted cells.
        // The approved samples use the standard 1900 date system; the 1899-12-30 base accounts for Excel's leap bug.
        if (normalized.matches("\\d+(?:\\.\\d+)?")) {
            try {
                long serialDays = new BigDecimal(normalized).longValueExact();
                if (serialDays >= 1 && serialDays <= 1_000_000) {
                    return LocalDate.of(1899, 12, 30).plusDays(serialDays);
                }
            } catch (ArithmeticException | NumberFormatException ignored) {}
        }
        return null;
    }

    private static String safeText(String raw, int max) {
        String value = raw == null ? "" : raw.strip();
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String value(List<String> row, int index) {
        return index < 0 || index >= row.size() ? "" : row.get(index);
    }

    private static final class HeaderMap {
        private final Map<String, Integer> columns = new HashMap<>();

        private HeaderMap(List<String> headers) {
            for (int i = 0; i < headers.size(); i++) {
                String normalized = SafeSpreadsheetReader.normalizeHeader(headers.get(i));
                if (!normalized.isBlank()) columns.putIfAbsent(normalized, i);
            }
        }

        int required(String... aliases) {
            int index = optional(aliases);
            if (index < 0) {
                throw new ManagedOperationValidationException("表格缺少必填列: " + aliases[0]);
            }
            return index;
        }

        int optional(String... aliases) {
            for (String alias : aliases) {
                Integer index = columns.get(SafeSpreadsheetReader.normalizeHeader(alias));
                if (index != null) return index;
            }
            return -1;
        }
    }
}
