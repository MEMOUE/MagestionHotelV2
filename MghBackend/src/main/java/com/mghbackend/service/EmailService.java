package com.mghbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:contact@magestionhotel.com}")
    private String fromEmail;

    @Value("${app.frontend-url:https://magestionhotel.com}")
    private String frontendUrl;

    /**
     * Envoie un email de réinitialisation de mot de passe.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token, String accountType, String recipientName) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String subject = "Réinitialisation de votre mot de passe — MaGestionHotel";

            String htmlContent = buildResetEmailHtml(recipientName, resetLink, accountType);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "MaGestionHotel");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Email de réinitialisation envoyé à {} (type: {})", toEmail, accountType);

        } catch (MessagingException e) {
            log.error("❌ Erreur envoi email de réinitialisation à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de réinitialisation");
        } catch (Exception e) {
            log.error("❌ Erreur inattendue envoi email à {} : {}", toEmail, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    private String buildResetEmailHtml(String name, String resetLink, String accountType) {
        String typeLabel = "HOTEL".equals(accountType) ? "Compte Hôtel" : "Compte Employé";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
            
                                <!-- Header -->
                                <tr>
                                    <td style="background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 100%%);padding:32px 40px;text-align:center;">
                                        <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;letter-spacing:1px;">
                                            🏨 MaGestionHotel
                                        </h1>
                                        <p style="color:#b8c5e8;margin:8px 0 0;font-size:14px;">
                                            Système de gestion hôtelière
                                        </p>
                                    </td>
                                </tr>
            
                                <!-- Body -->
                                <tr>
                                    <td style="padding:40px;">
                                        <h2 style="color:#1a1a2e;margin:0 0 16px;font-size:20px;">
                                            Réinitialisation de mot de passe
                                        </h2>
            
                                        <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 8px;">
                                            Bonjour <strong>%s</strong>,
                                        </p>
            
                                        <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 24px;">
                                            Nous avons reçu une demande de réinitialisation de mot de passe pour votre
                                            <strong>%s</strong>. Cliquez sur le bouton ci-dessous pour définir
                                            un nouveau mot de passe :
                                        </p>
            
                                        <!-- Button -->
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td align="center" style="padding:8px 0 32px;">
                                                    <a href="%s"
                                                       style="display:inline-block;background:#b8860b;color:#ffffff;text-decoration:none;
                                                              padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;
                                                              letter-spacing:0.5px;box-shadow:0 4px 12px rgba(184,134,11,0.3);">
                                                        Réinitialiser mon mot de passe
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
            
                                        <!-- Info -->
                                        <div style="background:#f8f6f0;border-left:4px solid #b8860b;padding:16px;border-radius:4px;margin-bottom:24px;">
                                            <p style="color:#666;font-size:13px;margin:0;line-height:1.5;">
                                                ⏰ Ce lien est valable pendant <strong>30 minutes</strong>.<br>
                                                🔒 Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.
                                            </p>
                                        </div>
            
                                        <!-- Fallback link -->
                                        <p style="color:#999;font-size:12px;line-height:1.5;margin:0;">
                                            Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br>
                                            <a href="%s" style="color:#b8860b;word-break:break-all;">%s</a>
                                        </p>
                                    </td>
                                </tr>
            
                                <!-- Footer -->
                                <tr>
                                    <td style="background:#f8f8f8;padding:24px 40px;border-top:1px solid #eee;text-align:center;">
                                        <p style="color:#999;font-size:12px;margin:0;line-height:1.5;">
                                            &copy; 2025 MaGestionHotel — Tous droits réservés<br>
                                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                                        </p>
                                    </td>
                                </tr>
            
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(name, typeLabel, resetLink, resetLink, resetLink);
    }
}