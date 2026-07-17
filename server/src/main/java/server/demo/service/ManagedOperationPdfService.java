package server.demo.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationSettings;
import server.demo.exception.ManagedOperationValidationException;

import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

@Service
public class ManagedOperationPdfService {
    private final String configuredFontPaths;
    private final ManagedOperationPrivateStampStorage stampStorage;

    public ManagedOperationPdfService(
            @Value("${registration.pdf.font-path:}") String configuredFontPaths,
            ManagedOperationPrivateStampStorage stampStorage) {
        this.configuredFontPaths = configuredFontPaths;
        this.stampStorage = stampStorage;
    }

    public ExportFile export(Long storeId, String documentType, ManagedOperationSettlementService.CalculationResult result) {
        if (!result.preview().exportAllowed()) {
            throw new ManagedOperationValidationException("当前预览存在阻断问题，不能导出");
        }
        String base = safeFilename(result.settings().getPropertyName());
        if (base.isBlank()) base = "managed-operation";
        base += "-" + result.settlementMonth();
        String normalizedType = documentType == null ? "" : documentType.toLowerCase(Locale.ROOT);
        validateDocumentFields(normalizedType, result.request());
        return switch (normalizedType) {
            case "settlement" -> new ExportFile(render(settlementHtml(result)), "application/pdf", base + "-精算書.pdf");
            case "invoice" -> new ExportFile(render(invoiceHtml(storeId, result, false)), "application/pdf", base + "-請求書.pdf");
            case "receipt" -> new ExportFile(render(invoiceHtml(storeId, result, true)), "application/pdf", base + "-領収書.pdf");
            case "all" -> new ExportFile(zip(storeId, result, base), "application/zip", base + "-documents.zip");
            default -> throw new ManagedOperationValidationException("不支持的文档类型");
        };
    }

    private byte[] zip(Long storeId, ManagedOperationSettlementService.CalculationResult result, String base) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            addZip(zip, base + "-精算書.pdf", render(settlementHtml(result)));
            addZip(zip, base + "-請求書.pdf", render(invoiceHtml(storeId, result, false)));
            addZip(zip, base + "-領収書.pdf", render(invoiceHtml(storeId, result, true)));
            zip.finish();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new ManagedOperationValidationException("ZIP 生成失败", ex);
        }
    }

    private static void addZip(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private byte[] render(String html) {
        Path font = resolveCjkFont(html);
        if (font == null) {
            throw new ManagedOperationValidationException(
                    "服务器未配置可用的 CJK 字体，已拒绝生成可能乱码的 PDF");
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.useFont(font.toFile(), "managed-cjk", 400,
                    BaseRendererBuilder.FontStyle.NORMAL, shouldSubset(font));
            builder.toStream(output);
            builder.run();
            byte[] bytes = output.toByteArray();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
                throw new ManagedOperationValidationException("PDF 渲染结果无效");
            }
            return bytes;
        } catch (ManagedOperationValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ManagedOperationValidationException("PDF 生成失败: " + ex.getMessage(), ex);
        }
    }

    private String settlementHtml(ManagedOperationSettlementService.CalculationResult result) {
        ManagedOperationDtos.PreviewSummary s = result.preview().summary();
        StringBuilder html = pageStart("landscape", "精算書");
        html.append("<div class='title'>").append(escape(result.settlementMonth() + " 精算書")).append("</div>")
                .append("<div class='sub'>").append(escape(result.settings().getPropertyName())).append("</div>")
                .append("<table class='settlement-table'><thead><tr><th>予約サイト</th><th>予約番号</th><th>チェックイン</th><th>チェックアウト</th><th>ゲスト名</th><th>部屋</th><th>通貨</th>")
                .append("<th>販売金額</th><th>OTAサービス料</th><th>OTA振込手数料</th><th>クリーニング代</th><th>受取金</th><th>管理費</th><th>振込予定金額</th><th>入金日</th><th>入金番号</th></tr></thead><tbody>");
        for (ManagedOperationDtos.PreviewLine line : result.preview().lines()) {
            if (line.status() != ManagedOperationDtos.LineStatus.INCLUDED) continue;
            String paymentReference = "AIRBNB".equals(line.platform()) ? line.bookingKey()
                    : (line.payoutReference() == null || line.payoutReference().isBlank() ? line.bookingKey() : line.payoutReference());
            html.append("<tr><td>").append(escape(line.platform())).append("</td><td>").append(escape(line.bookingKey()))
                    .append("</td><td>").append(escape(line.checkInDate())).append("</td><td>").append(escape(line.checkOutDate()))
                    .append("</td><td>").append(escape(line.guestName())).append("</td><td>")
                    .append(escape(line.roomNumber())).append("</td><td>").append(escape(line.currency()))
                    .append("</td><td class='num'>").append(yen(line.grossSales()))
                    .append("</td><td class='num'>").append(yen(line.otaServiceFee())).append("</td><td class='num'>")
                    .append(yen(line.payoutFee())).append("</td><td class='num'>").append(yen(line.cleaningFeeNet()))
                    .append("</td><td class='num'>").append(yen(line.receivedAmount()))
                    .append("</td><td class='num'>").append(yen(line.managementFee())).append("</td><td class='num'>")
                    .append(yen(line.scheduledTransfer())).append("</td><td>").append(escape(line.payoutDate()))
                    .append("</td><td>").append(escape(paymentReference)).append("</td></tr>");
        }
        html.append("</tbody></table><div class='summary'><table>")
                .append(summaryRow("受取金合計", s.totalReceived()))
                .append(summaryRow("管理費", s.managementFeeNet()))
                .append(summaryRow("清掃費消費税", s.cleaningTax()))
                .append(summaryRow("管理費消費税", s.managementTax()))
                .append(summaryRow("精算小計", s.settlementSubtotal()))
                .append(summaryRow("宿泊者名簿作成費（税込）", s.registrationFeeGross()));
        if (result.request().deductions() != null) {
            for (ManagedOperationDtos.DeductionInput deduction : result.request().deductions()) {
                html.append(summaryRow(escape(deduction.description()), deduction.amountGross()));
            }
        }
        html.append(summaryRow("振込確定金額", s.finalTransfer()))
                .append("</table></div>").append(note(result.request().note())).append(pageEnd());
        return html.toString();
    }

    private String invoiceHtml(Long storeId, ManagedOperationSettlementService.CalculationResult result, boolean receipt) {
        ManagedOperationSettings settings = result.settings();
        ManagedOperationDtos.PreviewSummary s = result.preview().summary();
        String title = receipt ? "領収書" : "請求書";
        String number = receipt ? result.request().receiptNumber() : result.request().invoiceNumber();
        LocalDate date = receipt ? result.request().receiptDate() : result.request().invoiceDate();
        String statement = receipt ? "下記の金額、正に領収いたしました。" : "下記の通りご請求申し上げます。";
        StringBuilder html = pageStart("portrait", title);
        html.append("<div class='banner'>").append(title).append("</div>")
                .append("<div class='docmeta'>No. ").append(escape(number)).append("<br/>").append(escape(date)).append("</div>")
                .append("<div class='party owner'><strong>").append(escape(settings.getOwnerCompanyName()));
        if (settings.getOwnerContactName() == null || settings.getOwnerContactName().isBlank()) {
            html.append(" 御中");
        }
        html.append("</strong>");
        if (settings.getOwnerContactName() != null && !settings.getOwnerContactName().isBlank()) {
            html.append("<br/>").append(escape(settings.getOwnerContactName()));
        }
        appendPostalAndAddress(html, settings.getOwnerPostalCode(), settings.getOwnerAddress());
        html.append("</div>")
                .append("<div class='amount'>合計金額　¥").append(yen(s.invoiceTotalGross())).append("（税込）</div>")
                .append("<div class='party issuer'><strong>").append(escape(settings.getIssuerCompanyName())).append("</strong>");
        appendPostalAndAddress(html, settings.getIssuerPostalCode(), settings.getIssuerAddress());
        html.append("<br/>登録番号: ").append(escape(settings.getIssuerRegistrationNumber()))
                .append("<br/>TEL: ").append(escape(settings.getIssuerPhone())).append("<br/>").append(escape(settings.getIssuerEmail()));
        String stamp = stampDataUri(storeId, settings.getStampStorageKey());
        if (!stamp.isBlank()) html.append("<img class='stamp' src='").append(stamp).append("'/>");
        html.append("</div><div class='statement'>").append(statement).append("</div>")
                .append("<table class='items'><thead><tr><th>品目</th><th>数量</th><th>単価</th><th>金額</th></tr></thead><tbody>")
                .append(itemRow(settings.getPropertyName() + " 管理手数料", 1, s.managementFeeNet(), s.managementFeeNet()))
                .append(itemRow("清掃費", s.includedReservationCount(), s.cleaningFeeNetUnit(), s.cleaningFeeNetTotal()))
                .append(itemRow("宿泊者名簿作成費", s.selectedRoomCount(),
                        settings.getRegistrationFeeNet(), s.registrationFeeNet()))
                .append("</tbody></table><div class='summary'><table>")
                .append(summaryRow("小計（税抜）", s.invoiceSubtotalNet()))
                .append(summaryRow("消費税", s.invoiceTax()))
                .append(summaryRow("合計（税込）", s.invoiceTotalGross()))
                .append("</table></div>");
        if (!receipt) {
            html.append("<div class='bank'><strong>振込先</strong><br/>")
                    .append(escape(settings.getBankName())).append("　").append(escape(settings.getBankBranch()))
                    .append("<br/>").append(escape(settings.getBankAccountType())).append("　")
                    .append(escape(settings.getBankAccountNumber())).append("　").append(escape(settings.getBankAccountHolder()));
            if (result.request().paymentDueDate() != null) {
                html.append("<br/>支払期限: ").append(escape(result.request().paymentDueDate()));
            }
            html.append("</div>");
        }
        html.append(note(result.request().note())).append(pageEnd());
        return html.toString();
    }

    private String stampDataUri(Long storeId, String key) {
        if (key == null || key.isBlank()) return "";
        ManagedOperationPrivateStampStorage.StoredStamp stamp = stampStorage.load(storeId, key);
        return "data:" + stamp.contentType() + ";base64," + Base64.getEncoder().encodeToString(stamp.bytes());
    }

    private static StringBuilder pageStart(String orientation, String title) {
        return new StringBuilder("<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>")
                .append(escape(title)).append("</title><style>")
                .append("@page{size:A4 ").append(orientation).append(";margin:12mm;}body{font-family:managed-cjk,sans-serif;color:#253247;font-size:10px;}")
                .append(".title{text-align:center;font-size:24px;font-weight:bold;margin:0 0 4px}.sub{text-align:center;margin-bottom:12px}.banner{background:#235b8e;color:#fff;text-align:center;font-size:25px;padding:10px;margin-bottom:18px}")
                .append("table{width:100%;border-collapse:collapse}thead{display:table-header-group}th{background:#dceaf5}th,td{border:1px solid #8aa2b5;padding:4px}.settlement-table{table-layout:fixed;font-size:7px}.settlement-table th,.settlement-table td{padding:2px;word-wrap:break-word}.num{text-align:right;white-space:nowrap}.summary{width:48%;margin:12px 0 0 auto}.summary td:first-child{background:#eef5fa}.summary td:last-child{text-align:right;font-weight:bold}")
                .append(".docmeta{text-align:right;line-height:1.7}.party{line-height:1.7;margin:8px 0}.owner{font-size:13px;width:48%}.issuer{position:relative;margin-left:55%;min-height:95px}.amount{border-bottom:2px solid #235b8e;font-size:18px;font-weight:bold;padding:12px 4px;margin:16px 0}.statement{margin-top:14px}.items{margin-top:10px}.stamp{position:absolute;max-width:72px;max-height:72px;right:0;bottom:0}.bank,.note{margin-top:16px;line-height:1.7;white-space:pre-wrap}.summary tr:last-child td{border-top:2px solid #235b8e}</style></head><body>");
    }

    private static String pageEnd() { return "</body></html>"; }

    private static String summaryRow(String label, BigDecimal amount) {
        return "<tr><td>" + label + "</td><td>¥" + yen(amount) + "</td></tr>";
    }

    private static String itemRow(String label, int quantity, BigDecimal unit, BigDecimal amount) {
        return "<tr><td>" + escape(label) + "</td><td class='num'>" + quantity + "</td><td class='num'>¥"
                + yen(unit) + "</td><td class='num'>¥" + yen(amount) + "</td></tr>";
    }

    private static String note(String note) {
        return note == null || note.isBlank() ? "" : "<div class='note'><strong>備考</strong><br/>" + escape(note) + "</div>";
    }

    private static void appendPostalAndAddress(StringBuilder html, String postalCode, String address) {
        if (postalCode != null && !postalCode.isBlank()) {
            html.append("<br/>〒").append(escape(postalCode));
        }
        if (address != null && !address.isBlank()) {
            html.append("<br/>").append(escape(address));
        }
    }

    private Path resolveCjkFont(String html) {
        String requiredCharacters = requiredVisibleCharacters(html);
        List<String> candidates = new ArrayList<>();
        if (configuredFontPaths != null) {
            for (String path : configuredFontPaths.replace(';', ',').split(",")) {
                if (!path.isBlank()) candidates.add(path.strip());
            }
        }
        candidates.addAll(List.of(
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc",
                "/System/Library/Fonts/ヒラギノ角ゴシック W6.ttc",
                "/System/Library/Fonts/Hiragino Sans GB.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJKjp-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansCJKjp-Regular.otf",
                "C:/Windows/Fonts/msgothic.ttc",
                "C:/Windows/Fonts/meiryo.ttc"));
        for (String candidate : candidates) {
            try {
                Path path = Path.of(candidate).toAbsolutePath().normalize();
                if (Files.isRegularFile(path) && Files.isReadable(path)
                        && isEmbeddableCjk(path, requiredCharacters)) return path;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean shouldSubset(Path font) {
        return fontKind(font) != FontKind.CFF;
    }

    private static boolean isEmbeddableCjk(Path font, String requiredCharacters) {
        if (fontKind(font) == FontKind.TTC_CFF) return false;
        try {
            Font[] fonts = Font.createFonts(font.toFile());
            for (Font candidate : fonts) {
                if (candidate.canDisplayUpTo(requiredCharacters) < 0) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    static String requiredVisibleCharacters(String html) {
        StringBuilder visibleText = new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                private int hiddenDepth;

                @Override
                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                    if (isNonVisibleTag(tag)) hiddenDepth++;
                }

                @Override
                public void handleEndTag(HTML.Tag tag, int position) {
                    if (isNonVisibleTag(tag) && hiddenDepth > 0) hiddenDepth--;
                }

                @Override
                public void handleText(char[] data, int position) {
                    if (hiddenDepth == 0) visibleText.append(data);
                }
            }, true);
        } catch (IOException ex) {
            throw new IllegalStateException("HTML 可见文本解析失败", ex);
        }

        StringBuilder requiredCharacters = new StringBuilder();
        visibleText.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint)
                        && !Character.isSpaceChar(codePoint)
                        && !Character.isISOControl(codePoint))
                .distinct()
                .forEach(requiredCharacters::appendCodePoint);
        return requiredCharacters.toString();
    }

    private static boolean isNonVisibleTag(HTML.Tag tag) {
        return tag == HTML.Tag.HEAD || tag == HTML.Tag.STYLE || tag == HTML.Tag.SCRIPT;
    }

    private static FontKind fontKind(Path font) {
        try (SeekableByteChannel channel = Files.newByteChannel(font)) {
            ByteBuffer header = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
            readFully(channel, header, 0);
            int tag = header.getInt(0);
            if (tag == 0x4f54544f) { // OTTO: OpenType CFF
                return FontKind.CFF;
            }
            if (tag == 0x74746366) { // ttcf: inspect the first font in the collection
                long firstOffset = Integer.toUnsignedLong(header.getInt(12));
                ByteBuffer sfnt = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                readFully(channel, sfnt, firstOffset);
                return sfnt.getInt(0) == 0x4f54544f ? FontKind.TTC_CFF : FontKind.TRUETYPE;
            }
            return FontKind.TRUETYPE;
        } catch (Exception ignored) {
            return FontKind.UNKNOWN;
        }
    }

    private static void readFully(SeekableByteChannel channel, ByteBuffer buffer, long position) throws IOException {
        buffer.clear();
        channel.position(position);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw new IOException("Unexpected EOF in font");
        }
        buffer.flip();
    }

    private enum FontKind { TRUETYPE, CFF, TTC_CFF, UNKNOWN }

    private static String yen(BigDecimal value) {
        if (value == null) return "";
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.JAPAN);
        format.setGroupingUsed(true);
        return format.format(value);
    }

    private static String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String safeFilename(String value) {
        if (value == null) return "";
        String safe = value.strip().replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", "-").replaceAll("-+", "-");
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }

    private static void validateDocumentFields(String type, ManagedOperationDtos.RunRequest request) {
        if (!("settlement".equals(type) || "invoice".equals(type) || "receipt".equals(type) || "all".equals(type))) {
            throw new ManagedOperationValidationException("不支持的文档类型");
        }
        if ("invoice".equals(type) || "all".equals(type)) {
            if (request.invoiceDate() == null || request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
                throw new ManagedOperationValidationException("导出请款书前必须填写请款日期和编号");
            }
        }
        if ("receipt".equals(type) || "all".equals(type)) {
            if (request.receiptDate() == null || request.receiptNumber() == null || request.receiptNumber().isBlank()) {
                throw new ManagedOperationValidationException("导出收据前必须填写收据日期和编号");
            }
        }
    }

    public record ExportFile(byte[] bytes, String contentType, String filename) {}
}
