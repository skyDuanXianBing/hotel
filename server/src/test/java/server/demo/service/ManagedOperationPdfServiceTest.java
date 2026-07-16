package server.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationSettings;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedOperationPdfServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void allExport_shouldContainSearchableLandscapeAndPortraitPdfs() throws Exception {
        String font = "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc";
        ManagedOperationPdfService service = new ManagedOperationPdfService(
                font, new ManagedOperationPrivateStampStorage(tempDir.toString()));
        ManagedOperationSettlementService.CalculationResult result = fixture();

        ManagedOperationPdfService.ExportFile export = service.export(1L, "all", result);

        assertEquals("application/zip", export.contentType());
        List<PdfEntry> entries = unzip(export.bytes());
        assertEquals(3, entries.size());
        for (PdfEntry entry : entries) {
            try (PDDocument document = PDDocument.load(entry.bytes())) {
                float width = document.getPage(0).getMediaBox().getWidth();
                float height = document.getPage(0).getMediaBox().getHeight();
                String text = new PDFTextStripper().getText(document);
                if (entry.name().contains("精算書")) {
                    assertTrue(width > height);
                    assertTrue(text.contains("精算書"));
                    assertTrue(text.contains("クリーニング代"));
                    assertTrue(text.contains("入金番号"));
                } else {
                    assertTrue(height > width);
                    assertTrue(text.contains(entry.name().contains("請求書") ? "請求書" : "領収書"));
                    assertTrue(text.contains("たんぽぽ株式会社 管理手数料"));
                    assertTrue(text.contains(entry.name().contains("請求書")
                            ? "下記の通りご請求申し上げます。"
                            : "下記の金額、正に領収いたしました。"));
                }
            }
        }
    }

    @Test
    void blockedPreview_shouldNeverGeneratePdf() {
        ManagedOperationPdfService service = new ManagedOperationPdfService(
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                new ManagedOperationPrivateStampStorage(tempDir.toString()));
        ManagedOperationSettlementService.CalculationResult fixture = fixture();
        ManagedOperationDtos.PreviewResponse blocked = new ManagedOperationDtos.PreviewResponse(
                fixture.preview().lines(), fixture.preview().stats(), fixture.preview().summary(), false,
                List.of("存在未匹配订单"));
        ManagedOperationSettlementService.CalculationResult result = new ManagedOperationSettlementService.CalculationResult(
                blocked, fixture.settings(), fixture.selectedRooms(), fixture.request(), fixture.settlementMonth());

        assertThrows(server.demo.exception.ManagedOperationValidationException.class,
                () -> service.export(1L, "settlement", result));
    }

    private static ManagedOperationSettlementService.CalculationResult fixture() {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setPropertyName("たんぽぽ株式会社");
        settings.setManagementFeeRate(new BigDecimal("0.10"));
        settings.setTaxRate(new BigDecimal("0.10"));
        settings.setCleaningFeeGross(new BigDecimal("8000"));
        settings.setRegistrationFeeNet(new BigDecimal("2000"));
        settings.setOwnerCompanyName("房東株式会社");
        settings.setOwnerContactName("山田 太郎");
        settings.setOwnerPostalCode("100-0001");
        settings.setOwnerAddress("東京都千代田区");
        settings.setIssuerCompanyName("運営株式会社");
        settings.setIssuerPostalCode("115-0001");
        settings.setIssuerAddress("東京都北区");
        settings.setIssuerRegistrationNumber("T1234567890123");
        settings.setIssuerPhone("03-0000-0000");
        settings.setIssuerEmail("test@example.com");
        settings.setBankName("テスト銀行");
        settings.setBankBranch("赤羽支店");
        settings.setBankAccountType("普通");
        settings.setBankAccountNumber("1234567");
        settings.setBankAccountHolder("ウンエイカブシキガイシャ");

        ManagedOperationDtos.PreviewLine line = new ManagedOperationDtos.PreviewLine(
                "BOOKING", 2, "5425310803", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2),
                "宿泊 太郎", "103", "JPY", new BigDecimal("14601"), new BigDecimal("2628"),
                new BigDecimal("336"), new BigDecimal("7273"), new BigDecimal("4364"),
                new BigDecimal("436"), new BigDecimal("3928"), LocalDate.of(2026, 6, 1), "PAY-1",
                ManagedOperationDtos.LineStatus.INCLUDED, List.of());
        ManagedOperationDtos.PreviewSummary summary = new ManagedOperationDtos.PreviewSummary(
                1, 1, new BigDecimal("4364"), new BigDecimal("436"), new BigDecimal("7273"),
                new BigDecimal("7273"), new BigDecimal("727"), new BigDecimal("44"),
                new BigDecimal("3157"), new BigDecimal("2000"), new BigDecimal("2200"),
                BigDecimal.ZERO, new BigDecimal("957"), new BigDecimal("9709"),
                new BigDecimal("970"), new BigDecimal("10679"));
        EnumMap<ManagedOperationDtos.LineStatus, Integer> counts = new EnumMap<>(ManagedOperationDtos.LineStatus.class);
        for (ManagedOperationDtos.LineStatus status : ManagedOperationDtos.LineStatus.values()) counts.put(status, 0);
        counts.put(ManagedOperationDtos.LineStatus.INCLUDED, 1);
        ManagedOperationDtos.PreviewResponse preview = new ManagedOperationDtos.PreviewResponse(
                List.of(line), new ManagedOperationDtos.PreviewStats(0, 1, counts), summary, true, List.of());
        ManagedOperationDtos.RunRequest request = new ManagedOperationDtos.RunRequest(
                "2026-05", List.of(), "INV-1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                "REC-1", LocalDate.of(2026, 6, 1), "テスト備考");
        return new ManagedOperationSettlementService.CalculationResult(
                preview, settings, List.of(new server.demo.entity.Room()), request, YearMonth.of(2026, 5));
    }

    private static List<PdfEntry> unzip(byte[] bytes) throws Exception {
        List<PdfEntry> result = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                result.add(new PdfEntry(entry.getName(), zip.readAllBytes()));
            }
        }
        return result;
    }

    private record PdfEntry(String name, byte[] bytes) {}
}
