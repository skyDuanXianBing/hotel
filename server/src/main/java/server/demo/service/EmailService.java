package server.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 * 负责发送邮件
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     *
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param type 类型（login/register/reset_password）
     * @throws MessagingException 邮件发送异常
     */
    public void sendVerificationCode(String toEmail, String code, String type) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);

        String subject = getSubjectByType(type);
        String content = buildEmailContent(code, type);

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    /**
     * 根据类型获取邮件主题
     *
     * @param type 类型
     * @return 邮件主题
     */
    private String getSubjectByType(String type) {
        return switch (type) {
            case "login" -> "【订单管理系统】登录验证码";
            case "register" -> "【订单管理系统】注册验证码";
            case "reset_password" -> "【订单管理系统】重置密码验证码";
            default -> "【订单管理系统】验证码";
        };
    }

    /**
     * 构建邮件内容
     *
     * @param code 验证码
     * @param type 类型
     * @return 邮件内容
     */
    private String buildEmailContent(String code, String type) {
        String purpose = switch (type) {
            case "login" -> "登录";
            case "register" -> "注册";
            case "reset_password" -> "重置密码";
            default -> "验证";
        };

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #1e88e5 0%%, #1976d2 100%%);
                            color: white;
                            padding: 20px;
                            text-align: center;
                            border-radius: 5px 5px 0 0;
                        }
                        .content {
                            background: #f9f9f9;
                            padding: 30px;
                            border-radius: 0 0 5px 5px;
                        }
                        .code-box {
                            background: white;
                            border: 2px dashed #1976d2;
                            padding: 20px;
                            text-align: center;
                            margin: 20px 0;
                            border-radius: 5px;
                        }
                        .code {
                            font-size: 32px;
                            font-weight: bold;
                            color: #1976d2;
                            letter-spacing: 5px;
                        }
                        .footer {
                            margin-top: 20px;
                            padding-top: 20px;
                            border-top: 1px solid #ddd;
                            font-size: 12px;
                            color: #999;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>订单管理系统</h1>
                        </div>
                        <div class="content">
                            <p>您好，</p>
                            <p>您正在进行<strong>%s</strong>操作，您的验证码是：</p>
                            <div class="code-box">
                                <div class="code">%s</div>
                            </div>
                            <p>验证码有效期为 <strong>5分钟</strong>，请尽快使用。</p>
                            <p>如果这不是您本人的操作，请忽略此邮件。</p>
                            <div class="footer">
                                <p>此邮件由系统自动发送，请勿直接回复。</p>
                                <p>&copy; 2025 订单管理系统. All rights reserved.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(purpose, code);
    }
}
