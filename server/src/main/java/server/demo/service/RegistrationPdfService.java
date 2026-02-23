package server.demo.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.RegistrationAttachmentType;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.StoreRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistrationPdfService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationPdfService.class);

    private final String fontPath;
    private final RegistrationAttachmentRepository registrationAttachmentRepository;
    private final StoreRepository storeRepository;

    public RegistrationPdfService(
            @Value("${registration.pdf.font-path:}") String fontPath,
            RegistrationAttachmentRepository registrationAttachmentRepository,
            StoreRepository storeRepository
    ) {
        this.fontPath = fontPath;
        this.registrationAttachmentRepository = registrationAttachmentRepository;
        this.storeRepository = storeRepository;
    }

    public byte[] render(RegistrationForm form, Reservation reservation, List<RegistrationGuest> guests) {
        String html = buildHtml(form, reservation, guests);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.useFastMode();

            List<FontCandidate> fonts = resolveFontCandidates();
            if (fonts.isEmpty()) {
                logger.warn("[RegistrationPdf][Font] no font candidates found. fontPath={}", fontPath);
            }
            for (FontCandidate c : fonts) {
                FontEmbedDecision decision = decideFontEmbedding(c.path());
                boolean subset = decision.subset();
                builder.useFont(c.path().toFile(), c.family(), 400, BaseRendererBuilder.FontStyle.NORMAL, subset);
                logger.info("[RegistrationPdf][Font] useFont family={}, path={}, subset={}, kind={}", c.family(), c.path(), subset, decision.kind());
            }

            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    private String buildHtml(RegistrationForm form, Reservation reservation, List<RegistrationGuest> guests) {
        List<RegistrationAttachment> atts = registrationAttachmentRepository.findByFormId(form.getId());
        Map<Long, RegistrationAttachment> passportByGuestId = new HashMap<>();
        if (atts != null) {
            for (RegistrationAttachment a : atts) {
                if (a.getGuest() != null && a.getType() == RegistrationAttachmentType.PASSPORT) {
                    passportByGuestId.putIfAbsent(a.getGuest().getId(), a);
                }
            }
        }

        String storeName = "";
        if (form != null && form.getStoreId() != null) {
            Optional<Store> store = storeRepository.findById(form.getStoreId());
            storeName = store.map(Store::getName).orElse("");
        }

        Room room = reservation != null ? reservation.getRoom() : null;
        RoomType roomType = (room != null) ? room.getRoomType() : null;
        String roomTypeName = roomType != null ? nvl(roomType.getName()) : "";
        String roomNumber = room != null ? nvl(room.getRoomNumber()) : nvl(reservation != null ? reservation.getOtaRoomNumber() : null);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'/>");
        sb.append("<style>");
        sb.append("body{font-family:custom, custom2, custom3, cjk, jp, kr, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:12px; color:#111;}");
        sb.append(".lang-en{font-family:custom, custom2, custom3, cjk, jp, kr, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;}");
        sb.append(".lang-ja{font-family:custom, custom2, custom3, jp, cjk, kr, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;}");
        sb.append(".header-table{width:100%;border-collapse:collapse;margin:0 0 10px 0;}");
        sb.append(".header-table td{border:none;padding:0;vertical-align:top;}");
        sb.append(".store{font-size:14px;font-weight:800;text-align:left;}");
        sb.append(".title{font-size:16px;font-weight:900;text-align:center;}");
        sb.append(".roomline{font-size:12px;font-weight:700;text-align:right;}");
        sb.append(".section-title{font-size:13px;font-weight:900;margin:14px 0 6px 0;}");
        sb.append("table{width:100%;border-collapse:collapse;}");
        sb.append("td{border:1px solid #333;padding:8px;vertical-align:top;}");
        sb.append("td.label{width:46%;background:#f6f7f9;font-weight:700;}");
        sb.append("td.value{width:54%;}");
        sb.append(".val{white-space:pre-wrap;}");
        // Prevent important blocks (e.g., passport image row) from being split across pages.
        sb.append(".avoid-break{page-break-inside:avoid;break-inside:avoid;}");
        sb.append("tr.avoid-break{page-break-inside:avoid;break-inside:avoid;}");
        sb.append("td.avoid-break{page-break-inside:avoid;break-inside:avoid;}");
        sb.append(".passport-img{max-width:220px;max-height:160px;display:block;border:1px solid #ddd;}");
        sb.append("</style></head><body>");

        sb.append("<table class='header-table'>");
        sb.append("<tr>");
        sb.append("<td class='store' style='width:33%;'>").append(escape(storeName)).append("</td>");
        sb.append("<td class='title' style='width:34%;'>宿泊者名簿<br/>REGISTRATION FORM</td>");
        sb.append("<td class='roomline' style='width:33%;'>")
            .append("Booking Number: ").append(escape(reservation != null ? reservation.getOrderNumber() : ""))
            .append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<table>");
        sb.append(row2("Name registered when booking", "予約者名", reservation != null ? reservation.getGuestName() : ""));
        sb.append(row2("Check-In Day", "チェックイン日", addRandomTime(reservation != null && reservation.getCheckInDate() != null ? reservation.getCheckInDate().toString() : "")));
        sb.append(row2("Check-Out Day", "チェックアウト日", addRandomTime(reservation != null && reservation.getCheckOutDate() != null ? reservation.getCheckOutDate().toString() : "")));
        sb.append(row2("Room Number", "部屋番号", roomNumber));
        sb.append("</table>");

        int idx = 1;
        for (RegistrationGuest g : guests) {
            sb.append("<div class='section-title'>Guest ").append(idx++).append("</div>");
            sb.append("<table>");
            
            boolean isOther = (g.getResidenceType() == null) || "OTHER".equalsIgnoreCase(g.getResidenceType().name());
            
            if (isOther) {
                // Other地区布局
                sb.append(row2("Guest Name", "宿泊者名", (nvl(g.getLastName()) + " " + nvl(g.getFirstName())).trim()));
                sb.append(row2("Residence", "居住地", g.getResidenceType() == null ? "" : g.getResidenceType().name()));
                sb.append(row2("Date of Birth", "生年月日", g.getBirthday() != null ? g.getBirthday().toString() : ""));
                sb.append(row2("Phone number", "電話番号", g.getPhone()));
                
                // 综合地址信息
                String fullAddress = buildFullAddress(g);
                sb.append(row2("Home Address", "住所", fullAddress));
                
                sb.append(row2("Previous location", "前泊地", g.getPriorStay()));
                sb.append(row2("Next destination", "行先", g.getNextDestination()));
                sb.append(row2("Passport number", "旅券番号", g.getPassportNumber()));
                sb.append(row2("Nationality", "国籍", g.getNationality()));

                RegistrationAttachment passport = (g.getId() == null) ? null : passportByGuestId.get(g.getId());
                String img = passport == null ? "" : toDataUri(passport);
                String passportCell = img.isBlank() ? "" : ("<img class='passport-img' src='" + img + "'/>");
                sb.append(rowHtmlWithTrClass("avoid-break", labelHtml2("Passport photo", "旅券写真"), passportCell));
            } else {
                // Japan地区布局
                sb.append(row2("Guest Name", "宿泊者名", (nvl(g.getLastName()) + " " + nvl(g.getFirstName())).trim()));
                sb.append(row2("Residence", "居住地", g.getResidenceType() == null ? "" : g.getResidenceType().name()));
                sb.append(row2("Date of Birth", "生年月日", g.getBirthday() != null ? g.getBirthday().toString() : ""));
                sb.append(row2("Phone number", "電話番号", g.getPhone()));
                sb.append(row2("Home Address", "住所", g.getAddress()));
            }

            sb.append("</table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private record FontCandidate(String family, Path path) {}

    private record FontEmbedDecision(boolean subset, String kind) {}

    private static FontEmbedDecision decideFontEmbedding(Path fontPath) {
        // Goal: pick a setting that works on Linux servers with CJK fonts.
        // - PDFBox subsetting fails for CFF OpenType fonts: "OTF fonts do not have a glyf table" => subset=false
        // - Full embedding of TrueType Collections (TTC) is not supported => subset=true
        try {
            DetectedFontKind kind = detectFontKind(fontPath);
            return switch (kind) {
                case OTF_CFF -> new FontEmbedDecision(false, "otf_cff");
                case OTF_TTF -> new FontEmbedDecision(true, "otf_ttf");
                case TTC_TTF -> new FontEmbedDecision(true, "ttc_ttf");
                case TTC_CFF -> new FontEmbedDecision(false, "ttc_cff");
                case UNKNOWN -> new FontEmbedDecision(true, "unknown");
            };
        } catch (Exception e) {
            return new FontEmbedDecision(true, "unknown:" + e.getClass().getSimpleName());
        }
    }

    private enum DetectedFontKind {
        OTF_CFF,
        OTF_TTF,
        TTC_TTF,
        TTC_CFF,
        UNKNOWN
    }

    private static DetectedFontKind detectFontKind(Path fontPath) throws IOException {
        if (fontPath == null || !Files.exists(fontPath)) {
            return DetectedFontKind.UNKNOWN;
        }

        try (SeekableByteChannel ch = Files.newByteChannel(fontPath)) {
            ByteBuffer head = ByteBuffer.allocate(4);
            readFully(ch, head, 0);
            int tag = head.getInt(0);

            // 'OTTO' => OpenType CFF
            if (tag == 0x4F54544F) {
                return DetectedFontKind.OTF_CFF;
            }

            // 'ttcf' => TrueType/OpenType collection (TTC/OTC)
            if (tag == 0x74746366) {
                ByteBuffer ttcHeader = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
                readFully(ch, ttcHeader, 0);
                long numFonts = intToUnsignedLong(ttcHeader.getInt(8));
                if (numFonts <= 0) {
                    return DetectedFontKind.UNKNOWN;
                }
                long firstOffset = intToUnsignedLong(ttcHeader.getInt(12));

                ByteBuffer sfnt = ByteBuffer.allocate(4);
                readFully(ch, sfnt, firstOffset);
                int sfntTag = sfnt.getInt(0);
                if (sfntTag == 0x4F54544F) {
                    return DetectedFontKind.TTC_CFF;
                }
                // 0x00010000 is typical for TrueType outlines; other legacy tags exist, treat as TrueType-ish.
                return DetectedFontKind.TTC_TTF;
            }

            // 0x00010000 (or 'true', 'typ1') indicates TrueType outlines in an sfnt container.
            return DetectedFontKind.OTF_TTF;
        }
    }

    private static void readFully(SeekableByteChannel ch, ByteBuffer buf, long pos) throws IOException {
        buf.clear();
        ch.position(pos);
        while (buf.hasRemaining()) {
            int r = ch.read(buf);
            if (r < 0) {
                throw new IOException("Unexpected EOF while reading font file");
            }
        }
        buf.flip();
    }

    private static long intToUnsignedLong(int v) {
        return v & 0xFFFF_FFFFL;
    }

    private static List<String> splitFontPaths(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        // Support comma/semicolon separated font paths, e.g.:
        // /path/to/uming.ttc,/path/to/gulim.ttf
        String normalized = raw.replace(";", ",");
        String[] parts = normalized.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private List<FontCandidate> resolveFontCandidates() {
        List<FontCandidate> out = new ArrayList<>();

        int customIndex = 0;
        for (String raw : splitFontPaths(fontPath)) {
            try {
                Path p = Path.of(raw);
                if (Files.exists(p)) {
                    customIndex++;
                    String family = (customIndex == 1) ? "custom" : ("custom" + customIndex);
                    out.add(new FontCandidate(family, p));
                } else {
                    logger.warn("[RegistrationPdf][Font] configured font path not found: {}", raw);
                }
            } catch (Exception e) {
                logger.warn("[RegistrationPdf][Font] invalid font path: {}", raw, e);
            }
        }

        String os = String.valueOf(System.getProperty("os.name", "")).toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            addIfExists(out, "cjk", Path.of("C:\\Windows\\Fonts\\msyh.ttc"));
            addIfExists(out, "cjk", Path.of("C:\\Windows\\Fonts\\msyh.ttf"));
            addIfExists(out, "cjk", Path.of("C:\\Windows\\Fonts\\simsun.ttc"));
            addIfExists(out, "cjk", Path.of("C:\\Windows\\Fonts\\simhei.ttf"));
            addIfExists(out, "jp", Path.of("C:\\Windows\\Fonts\\meiryo.ttc"));
            addIfExists(out, "jp", Path.of("C:\\Windows\\Fonts\\YuGothR.ttc"));
            addIfExists(out, "kr", Path.of("C:\\Windows\\Fonts\\malgun.ttf"));
        } else {
            addIfExists(out, "cjk", Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"));
            addIfExists(out, "cjk", Path.of("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"));
            addIfExists(out, "cjk", Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK.ttc"));
            addIfExists(out, "cjk", Path.of("/usr/share/fonts/truetype/noto/NotoSansCJK.ttc"));
        }

        List<FontCandidate> dedup = new ArrayList<>();
        for (FontCandidate c : out) {
            boolean exists = false;
            for (FontCandidate d : dedup) {
                if (d.path().equals(c.path())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                dedup.add(c);
            }
        }
        return dedup;
    }

    private void addIfExists(List<FontCandidate> out, String family, Path p) {
        try {
            if (p != null && Files.exists(p)) {
                out.add(new FontCandidate(family, p));
            }
        } catch (Exception ignored) {
        }
    }

    private String labelHtml2(String en, String ja) {
        return "<div>"
                + "<span class='lang-ja'>" + escape(ja) + "</span>"
                + "</div>";
    }

    private String row2(String en, String ja, String value) {
        return rowHtml(labelHtml2(en, ja), "<div class='val'>" + escape(value) + "</div>");
    }

    private String rowHtml(String labelHtml, String valueHtml) {
        return "<tr><td class='label'>" + labelHtml + "</td><td class='value'>" + valueHtml + "</td></tr>";
    }

    private String rowHtmlWithTrClass(String trClass, String labelHtml, String valueHtml) {
        String cls = (trClass == null || trClass.isBlank()) ? "" : (" class='" + escapeAttr(trClass) + "'");
        return "<tr" + cls + "><td class='label'>" + labelHtml + "</td><td class='value'>" + valueHtml + "</td></tr>";
    }

    private String escapeAttr(String s) {
        if (s == null) {
            return "";
        }
        // Conservative: only used for internal constant class names.
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String toDataUri(RegistrationAttachment att) {
        try {
            if (att == null || att.getFilePath() == null || att.getFilePath().isBlank()) {
                return "";
            }
            Path p = Path.of(att.getFilePath());
            if (!Files.exists(p)) {
                return "";
            }
            byte[] bytes = Files.readAllBytes(p);
            if (bytes.length == 0) {
                return "";
            }
            String ct = (att.getContentType() == null || att.getContentType().isBlank()) ? "image/jpeg" : att.getContentType();
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + ct + ";base64," + b64;
        } catch (Exception e) {
            return "";
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String buildFullAddress(RegistrationGuest g) {
        if (g == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (g.getAddress1() != null && !g.getAddress1().isBlank()) {
            parts.add(g.getAddress1());
        }
        if (g.getAddress2() != null && !g.getAddress2().isBlank()) {
            parts.add(g.getAddress2());
        }
        if (g.getCity() != null && !g.getCity().isBlank()) {
            parts.add(g.getCity());
        }
        if (g.getState() != null && !g.getState().isBlank()) {
            parts.add(g.getState());
        }
        if (g.getCountry() != null && !g.getCountry().isBlank()) {
            parts.add(g.getCountry());
        }
        return String.join(", ", parts);
    }

    private String addRandomTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return dateStr;
        }
        // 生成16:00-23:59之间的随机时间
        java.util.Random random = new java.util.Random();
        int hour = 16 + random.nextInt(8); // 16-23
        int minute = random.nextInt(60);   // 0-59
        return dateStr + String.format(" %02d:%02d", hour, minute);
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
