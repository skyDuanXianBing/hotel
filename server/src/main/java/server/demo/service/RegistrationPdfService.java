package server.demo.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class RegistrationPdfService {

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

            for (FontCandidate c : resolveFontCandidates()) {
                builder.useFont(c.path().toFile(), c.family());
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
        sb.append("body{font-family:cjk, custom, zh, jp, kr, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; font-size:12px; color:#111;}");
        sb.append(".header-table{width:100%;border-collapse:collapse;margin:0 0 10px 0;}");
        sb.append(".header-table td{border:none;padding:0;vertical-align:bottom;}");
        sb.append(".store{font-size:14px;font-weight:800;text-align:left;}");
        sb.append(".title{font-size:16px;font-weight:900;text-align:right;}");
        sb.append(".roomline{font-size:12px;font-weight:700;text-align:right;margin-top:2px;}");
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
        sb.append("<td class='store'>").append(escape(storeName)).append("</td>");
        sb.append("<td class='title'>REGISTRATION FORM</td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("<td></td>");
        sb.append("<td class='roomline'>")
            .append("Room Type: ").append(escape(roomTypeName))
            .append("&#160;&#160;Room Number: ").append(escape(roomNumber))
            .append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<table>");
        sb.append(row("Name registered when booking", "予約者名", "预约人姓名", "예약자 이름", reservation != null ? reservation.getGuestName() : ""));
        sb.append(row("Check-In Day", "チェックイン日", "入住日", "체크인 날짜", reservation != null && reservation.getCheckInDate() != null ? reservation.getCheckInDate().toString() : ""));
        sb.append(row("Check-Out Day", "チェックアウト日", "退房日", "체크아웃 날짜", reservation != null && reservation.getCheckOutDate() != null ? reservation.getCheckOutDate().toString() : ""));
        sb.append(row("Room Type", "部屋タイプ", "房型", "객실 유형", roomTypeName));
        sb.append(row("Room Number", "部屋番号", "房间号", "객실 번호", roomNumber));
        // requirement: if no data, show blank (no placeholder)
        sb.append(row("Travel Agent of your booking", "予約の旅行代理店", "预订旅行社", "예약 여행사", ""));
        sb.append(row("Order Number", "注文番号", "订单号", "주문 번호", reservation != null ? reservation.getOrderNumber() : ""));
        sb.append("</table>");

        int idx = 1;
        for (RegistrationGuest g : guests) {
            sb.append("<div class='section-title'>Guest ").append(idx++).append("</div>");
            sb.append("<table>");
            sb.append(row("Guest Name", "宿泊者名", "入住人姓名", "투숙자 이름", (nvl(g.getLastName()) + " " + nvl(g.getFirstName())).trim()));
            sb.append(row("Residence", "居住地", "居住地", "거주지", g.getResidenceType() == null ? "" : g.getResidenceType().name()));
            sb.append(row("Home Address", "住所", "住址", "주소", g.getAddress()));
            sb.append(row("Phone number", "電話番号", "电话号码", "전화번호", g.getPhone()));
            sb.append(row("Date of Birth", "生年月日", "出生日期", "생년월일", g.getBirthday() != null ? g.getBirthday().toString() : ""));

            boolean isOther = (g.getResidenceType() == null) || "OTHER".equalsIgnoreCase(g.getResidenceType().name());
            if (isOther) {
                sb.append(row("Nationality", "国籍", "国籍", "국적", g.getNationality()));
                sb.append(row("Passport number", "旅券番号", "护照号", "여권번호", g.getPassportNumber()));

                RegistrationAttachment passport = (g.getId() == null) ? null : passportByGuestId.get(g.getId());
                String img = passport == null ? "" : toDataUri(passport);
                String passportCell = img.isBlank() ? "" : ("<img class='passport-img' src='" + img + "'/>");
                sb.append(rowHtmlWithTrClass("avoid-break", labelHtml("Passport photo", "旅券写真", "护照照片", "여권사진"), passportCell));

                sb.append(row("Previous location", "前泊地", "前泊地", "전 숙박지", g.getPriorStay()));
                sb.append(row("Next destination", "行先", "行先", "다음 목적지", g.getNextDestination()));
            }

            sb.append("</table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private record FontCandidate(String family, Path path) {}

    private List<FontCandidate> resolveFontCandidates() {
        List<FontCandidate> out = new ArrayList<>();

        if (fontPath != null && !fontPath.isBlank() && Files.exists(Path.of(fontPath))) {
            out.add(new FontCandidate("custom", Path.of(fontPath)));
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

    private String labelHtml(String en, String ja, String zh, String ko) {
        return "<div>" + escape(en) + "<br/>" + escape(ja) + "<br/>" + escape(zh) + "<br/>" + escape(ko) + "</div>";
    }

    private String row(String en, String ja, String zh, String ko, String value) {
        return rowHtml(labelHtml(en, ja, zh, ko), "<div class='val'>" + escape(value) + "</div>");
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
