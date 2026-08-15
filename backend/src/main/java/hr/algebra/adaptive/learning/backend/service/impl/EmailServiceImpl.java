package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;
    private final boolean enabled;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@adaptivelearn.hr}") String fromAddress,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${app.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.enabled = enabled;
    }

    @Override
    @Async
    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;

        if (!enabled) {
            log.warn("Slanje e-poste je iskljuceno. Poveznica za {}: {}", user.getEmail(), link);
            return;
        }

        String html = buildVerificationHtml(user.getFirstName(), link);
        send(user.getEmail(), "Potvrdite svoju e-mail adresu - AdaptiveLearn", html);
    }

    @Override
    @Async
    public void sendAccountApprovedEmail(User user) {
        if (!enabled) {
            log.warn("Slanje e-poste je iskljuceno. Racun odobren: {}", user.getEmail());
            return;
        }

        String html = buildApprovedHtml(user.getFirstName(), frontendUrl + "/login");
        send(user.getEmail(), "Vas racun je aktiviran - AdaptiveLearn", html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_NO, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("E-mail poslan na: {}", to);
        } catch (Exception e) {
            log.error("Slanje e-poste na {} nije uspjelo: {}", to, e.getMessage());
        }
    }

    private String buildVerificationHtml(String firstName, String link) {
        return """
                <!DOCTYPE html>
                <html lang="hr">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Segoe UI,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:32px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;">
                        <tr>
                          <td style="background:#18181b;padding:24px 32px;">
                            <span style="color:#ffffff;font-size:20px;font-weight:600;">AdaptiveLearn</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <h1 style="margin:0 0 16px;font-size:22px;color:#18181b;">Pozdrav, %s!</h1>
                            <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#3f3f46;">
                              Zaprimili smo vasu registraciju. Kliknite na gumb ispod kako biste
                              potvrdili da ova e-mail adresa pripada vama.
                            </p>
                            <table cellpadding="0" cellspacing="0" style="margin:0 0 24px;">
                              <tr><td style="background:#2563eb;border-radius:8px;">
                                <a href="%s" style="display:inline-block;padding:13px 28px;color:#ffffff;
                                   text-decoration:none;font-size:15px;font-weight:600;">Potvrdi e-mail adresu</a>
                              </td></tr>
                            </table>
                            <p style="margin:0 0 8px;font-size:13px;color:#71717a;">
                              Ako gumb ne radi, kopirajte ovu poveznicu u preglednik:
                            </p>
                            <p style="margin:0 0 24px;font-size:13px;word-break:break-all;color:#2563eb;">%s</p>
                            <div style="padding:14px 16px;background:#fef3c7;border-left:4px solid #d97706;border-radius:4px;">
                              <p style="margin:0;font-size:13px;line-height:1.5;color:#3f3f46;">
                                <strong>Sto slijedi?</strong> Nakon potvrde e-mail adrese vas nastavnik
                                pregledava prijavu i dodjeljuje vas u razred. O aktivaciji racuna
                                bit cete obavijesteni e-mailom.
                              </p>
                            </div>
                            <p style="margin:24px 0 0;font-size:13px;color:#71717a;">
                              Poveznica vrijedi 24 sata. Ako se niste registrirali, zanemarite ovu poruku.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 32px;background:#fafafa;border-top:1px solid #e4e4e7;">
                            <p style="margin:0;font-size:12px;color:#a1a1aa;">
                              AdaptiveLearn - sustav za pracenje napretka u ucenju programiranja
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(firstName), link, link);
    }

    private String buildApprovedHtml(String firstName, String loginUrl) {
        return """
                <!DOCTYPE html>
                <html lang="hr">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Segoe UI,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:32px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;">
                        <tr>
                          <td style="background:#18181b;padding:24px 32px;">
                            <span style="color:#ffffff;font-size:20px;font-weight:600;">AdaptiveLearn</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <h1 style="margin:0 0 16px;font-size:22px;color:#18181b;">Racun je aktiviran</h1>
                            <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#3f3f46;">
                              Pozdrav, %s! Vas nastavnik odobrio je vasu prijavu i sada se mozete
                              prijaviti u sustav.
                            </p>
                            <table cellpadding="0" cellspacing="0">
                              <tr><td style="background:#10b981;border-radius:8px;">
                                <a href="%s" style="display:inline-block;padding:13px 28px;color:#ffffff;
                                   text-decoration:none;font-size:15px;font-weight:600;">Prijavi se</a>
                              </td></tr>
                            </table>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 32px;background:#fafafa;border-top:1px solid #e4e4e7;">
                            <p style="margin:0;font-size:12px;color:#a1a1aa;">
                              AdaptiveLearn - sustav za pracenje napretka u ucenju programiranja
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(firstName), loginUrl);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
