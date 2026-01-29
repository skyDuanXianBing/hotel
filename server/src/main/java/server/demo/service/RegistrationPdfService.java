package server.demo.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.RegistrationAttachment;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationAttachmentType;
import server.demo.repository.RegistrationAttachmentRepository;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationPdfService {

    private final String fontPath;
    private final RegistrationAttachmentRepository registrationAttachmentRepository;

    public RegistrationPdfService(
            @Value("${registration.pdf.font-path:}") String fontPath,
            RegistrationAttachmentRepository registrationAttachmentRepository
    ) {
        this.fontPath = fontPath;
        this.registrationAttachmentRepository = registrationAttachmentRepository;
    }

    public byte[] render(RegistrationForm form, Reservation reservation, List<RegistrationGuest> guests) {
        String html = buildHtml(form, reservation, guests);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.useFastMode();

            if (fontPath != null && !fontPath.isBlank() && Files.exists(Path.of(fontPath))) {
                builder.useFont(Path.of(fontPath).toFile(), "custom");
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

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'/>");
        sb.append("<style>");
        sb.append("body{font-family:custom, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, 'Noto Sans CJK SC', 'Noto Sans JP', sans-serif; font-size:12px;}");
        sb.append("h1{font-size:16px;margin:0 0 8px 0;}");
        sb.append(".meta{margin-bottom:10px;}");
        sb.append("table{width:100%;border-collapse:collapse;}");
        sb.append("th,td{border:1px solid #333;padding:6px;vertical-align:top;}");
        sb.append("th{background:#f2f2f2;}");
        sb.append(".small{font-size:10px;color:#444;}");
        sb.append(".passport-img{width:120px;max-height:90px;object-fit:contain;display:block;}");
        sb.append("</style></head><body>");

        sb.append("<h1>Guest Registration / 宿泊者名簿 / 入住登记</h1>");
        sb.append("<div class='meta'>");
        sb.append("<div>Order: ").append(escape(reservation.getOrderNumber())).append("</div>");
        sb.append("<div>Guest: ").append(escape(reservation.getGuestName())).append("</div>");
        sb.append("<div>Stay: ").append(reservation.getCheckInDate()).append(" ~ ").append(reservation.getCheckOutDate()).append("</div>");
        sb.append("<div>Status: ").append(form.getStatus()).append("</div>");
        sb.append("</div>");

        sb.append("<table>");
        sb.append("<tr>");
        sb.append("<th>#</th>");
        sb.append("<th>Name<br/><span class='small'>姓名 / 氏名</span></th>");
        sb.append("<th>Nationality<br/><span class='small'>国籍</span></th>");
        sb.append("<th>Residence<br/><span class='small'>居住地</span></th>");
        sb.append("<th>Passport No.<br/><span class='small'>旅券番号</span></th>");
        sb.append("<th>Prior / Next<br/><span class='small'>前泊地 / 行先</span></th>");
        sb.append("<th>Passport photo<br/><span class='small'>旅券写真 / 护照照片</span></th>");
        sb.append("</tr>");

        int i = 1;
        for (RegistrationGuest g : guests) {
            sb.append("<tr>");
            sb.append("<td>").append(i++).append("</td>");
            sb.append("<td>")
                    .append(escape(nvl(g.getLastName()))).append(" ").append(escape(nvl(g.getFirstName())))
                    .append("<br/><span class='small'>")
                    .append(escape(nvl(g.getLastNameKana()))).append(" ").append(escape(nvl(g.getFirstNameKana())))
                    .append("</span>")
                    .append("</td>");
            sb.append("<td>").append(escape(nvl(g.getNationality()))).append("</td>");
            sb.append("<td>").append(g.getResidenceType() == null ? "" : g.getResidenceType().name()).append("</td>");
            sb.append("<td>").append(escape(nvl(g.getPassportNumber()))).append("</td>");
            sb.append("<td>")
                    .append("<div>").append(escape(nvl(g.getPriorStay()))).append("</div>")
                    .append("<div>").append(escape(nvl(g.getNextDestination()))).append("</div>")
                    .append("</td>");

            RegistrationAttachment passport = (g.getId() == null) ? null : passportByGuestId.get(g.getId());
            String img = passport == null ? "" : toDataUri(passport);
            if (img.isBlank()) {
                sb.append("<td>-</td>");
            } else {
                sb.append("<td><img class='passport-img' src='").append(img).append("'/></td>");
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
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
