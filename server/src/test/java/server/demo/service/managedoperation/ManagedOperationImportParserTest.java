package server.demo.service.managedoperation;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import server.demo.exception.ManagedOperationValidationException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ManagedOperationImportParserTest {
    private final ManagedOperationImportParser parser = new ManagedOperationImportParser(new SafeSpreadsheetReader());

    @Test
    void airbnbCsv_shouldHandleBomQuotedCommaAndPayoutMetadata() {
        String csv = "\ufeff日付,入金予定日,種別,確認コード,開始日,終了日,ゲスト,リスティング,通貨,サービス料,総収入,参照コード\n"
                + "05/17/2026,05/22/2026,Payout,,,,,,, , ,REF-1\n"
                + "05/17/2026,,予約,HMABC123,05/16/2026,05/20/2026,\"Doe, Jane\",部屋204,JPY,\"6,675\",43064,\n";
        MockMultipartFile file = new MockMultipartFile("file", "airbnb.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ManagedOperationImportRow> rows = parser.parseAirbnb(file);

        assertEquals(1, rows.size());
        assertEquals("HMABC123", rows.get(0).bookingKey());
        assertEquals("Doe, Jane", rows.get(0).guestName());
        assertEquals("6675", rows.get(0).otaServiceFee().toPlainString());
        assertEquals("2026-05-22", rows.get(0).payoutDate().toString());
        assertEquals("REF-1", rows.get(0).payoutReference());
    }

    @Test
    void bookingXlsx_shouldReadFirstNonEmptySheetAndCachedFormulaWithoutEvaluator() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("empty");
            var sheet = workbook.createSheet("data");
            var header = sheet.createRow(0);
            String[] headers = {"予約番号", "チェックイン日", "Checkout", "宿泊者氏名", "通貨", "金額",
                    "コミッション", "決済サービスの手数料", "お支払い日", "お支払い番号"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(5425310803d);
            // Same shape as the approved Booking export: General-formatted Excel serial dates.
            row.createCell(1).setCellValue(46143);
            row.createCell(2).setCellValue(46144);
            row.createCell(3).setCellValue("Guest");
            row.createCell(4).setCellValue("JPY");
            var amount = row.createCell(5);
            amount.setCellFormula("14000+601");
            amount.setCellValue(14601);
            row.createCell(6).setCellValue(-2628);
            row.createCell(7).setCellValue(-336);
            row.createCell(8).setCellValue("2026-06-01");
            row.createCell(9).setCellValue("PAY-1");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        List<ManagedOperationImportRow> rows = parser.parseBooking(
                new MockMultipartFile("file", "booking.xlsx", "application/octet-stream", bytes));

        assertEquals(1, rows.size());
        assertEquals("5425310803", rows.get(0).bookingKey());
        assertEquals("14601", rows.get(0).grossSales().toPlainString());
        assertEquals("2026-05-01", rows.get(0).checkInDate().toString());
        assertEquals("2026-05-02", rows.get(0).checkOutDate().toString());
    }

    @Test
    void csv_shouldRejectInvalidUtf8AndSignatureMismatch() {
        assertThrows(ManagedOperationValidationException.class, () -> parser.parseBooking(
                new MockMultipartFile("file", "bad.csv", "text/csv", new byte[]{(byte) 0xc3, 0x28})));
        assertThrows(ManagedOperationValidationException.class, () -> parser.parseBooking(
                new MockMultipartFile("file", "fake.xlsx", "application/octet-stream", "not zip".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void nonJpy_shouldBeRejected() {
        String csv = "予約番号,チェックイン日,Checkout,宿泊者氏名,通貨,金額,コミッション,決済サービスの手数料\n"
                + "123,2026-05-01,2026-05-02,Guest,USD,100,10,2\n";
        assertThrows(ManagedOperationValidationException.class, () -> parser.parseBooking(
                new MockMultipartFile("file", "booking.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void wholeYen_shouldAllowZeroFractionAndRejectNonZeroFraction() {
        String allowed = "予約番号,チェックイン日,Checkout,宿泊者氏名,通貨,金額,コミッション,決済サービスの手数料\n"
                + "123,2026-05-01,2026-05-02,Guest,JPY,37187.00,-6675.00,0.00\n";
        List<ManagedOperationImportRow> rows = parser.parseBooking(new MockMultipartFile(
                "file", "booking.csv", "text/csv", allowed.getBytes(StandardCharsets.UTF_8)));
        assertEquals("37187.00", rows.get(0).grossSales().toPlainString());

        String rejected = "予約番号,チェックイン日,Checkout,宿泊者氏名,通貨,金額,コミッション,決済サービスの手数料\n"
                + "123,2026-05-01,2026-05-02,Guest,JPY,37187.50,-6675,0\n";
        assertThrows(ManagedOperationValidationException.class, () -> parser.parseBooking(new MockMultipartFile(
                "file", "booking.csv", "text/csv", rejected.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void bookingKeyNormalization_shouldRemoveInternalAndFullWidthWhitespace() {
        assertEquals("ABC123", ManagedOperationImportParser.normalizeBookingKey(" ＡＢ　 C １２３ "));
    }

    @Test
    void approvedRealSamples_shouldParseGeneralSerialDatesAndUtf8Bom() throws Exception {
        Path bookingPath = Path.of("..", "代运营", "booking输入原始数据.xlsx");
        Path airbnbPath = Path.of("..", "代运营", "airbnb输入原始数据.csv");
        assumeTrue(Files.isRegularFile(bookingPath) && Files.isRegularFile(airbnbPath),
                "local approved samples are not available in this environment");
        List<ManagedOperationImportRow> booking = parser.parseBooking(new MockMultipartFile(
                "file", bookingPath.getFileName().toString(), "application/octet-stream", Files.readAllBytes(bookingPath)));
        List<ManagedOperationImportRow> airbnb = parser.parseAirbnb(new MockMultipartFile(
                "file", airbnbPath.getFileName().toString(), "text/csv", Files.readAllBytes(airbnbPath)));

        assertEquals(26, booking.size());
        assertEquals("2026-05-31", booking.get(0).checkOutDate().toString());
        assertEquals(7, airbnb.size());
        assertEquals("2026-06-04", airbnb.get(0).checkOutDate().toString());
    }
}
