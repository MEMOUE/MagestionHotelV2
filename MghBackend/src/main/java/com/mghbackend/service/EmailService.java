package com.mghbackend.service;

import com.mghbackend.entity.Transaction;
import com.mghbackend.enums.TypeTransaction;
import com.mghbackend.exception.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
            throw new BusinessException("Impossible d'envoyer l'email de réinitialisation");
        } catch (Exception e) {
            log.error("❌ Erreur inattendue envoi email à {} : {}", toEmail, e.getMessage());
            throw new BusinessException("Erreur lors de l'envoi de l'email");
        }
    }

    /**
     * Envoie au propriétaire le rapport financier complet (entrées/sorties) du mois écoulé.
     * Ne relance jamais d'exception : une erreur d'envoi pour un hôtel ne doit pas
     * interrompre le traitement des autres hôtels dans la tâche planifiée appelante.
     */
    @Async
    public void sendMonthlyReportEmail(String toEmail, String hotelName, YearMonth mois,
                                        List<Transaction> transactions,
                                        BigDecimal totalRevenus, BigDecimal totalDepenses) {
        try {
            sendMonthlyReportEmailNow(toEmail, hotelName, mois, transactions, totalRevenus, totalDepenses);
        } catch (Exception e) {
            log.error("❌ Erreur envoi rapport financier mensuel à {} : {}", toEmail, e.getMessage());
        }
    }

    /**
     * Variante synchrone : propage les erreurs au lieu de les avaler.
     * Utilisée pour l'envoi à la demande par l'administrateur système, qui a besoin
     * d'un retour immédiat de succès/échec plutôt que d'un envoi silencieux en arrière-plan.
     */
    public void sendMonthlyReportEmailNow(String toEmail, String hotelName, YearMonth mois,
                                           List<Transaction> transactions,
                                           BigDecimal totalRevenus, BigDecimal totalDepenses) {
        String moisLabel = mois.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        try {
            String subject = "Rapport financier mensuel — " + moisLabel + " — " + hotelName;
            String htmlContent = buildMonthlyReportHtml(hotelName, moisLabel, transactions, totalRevenus, totalDepenses);
            byte[] pdf = renderHtmlToPdf(
                    buildMonthlyReportPdfHtml(hotelName, moisLabel, transactions, totalRevenus, totalDepenses));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "MaGestionHotel");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addAttachment(
                    "Rapport-financier-" + mois + ".pdf",
                    new ByteArrayResource(pdf)
            );

            mailSender.send(message);

            log.info("✅ Rapport financier mensuel (PDF) envoyé à {} pour {} ({})", toEmail, hotelName, moisLabel);
        } catch (Exception e) {
            log.error("❌ Erreur envoi rapport financier mensuel à {} : {}", toEmail, e.getMessage());
            throw new BusinessException("Impossible d'envoyer le rapport par email : " + e.getMessage());
        }
    }

    /**
     * Génère le PDF du rapport financier mensuel à partir d'un HTML dédié
     * (structure XHTML stricte requise par le moteur de rendu PDF).
     */
    private byte[] renderHtmlToPdf(String xhtml) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de la génération du PDF du rapport : " + e.getMessage());
        }
    }

    private String buildMonthlyReportPdfHtml(String hotelName, String moisLabel, List<Transaction> transactions,
                                              BigDecimal totalRevenus, BigDecimal totalDepenses) {
        BigDecimal solde = totalRevenus.subtract(totalDepenses);
        String soldeColor = solde.compareTo(BigDecimal.ZERO) >= 0 ? "#059669" : "#dc2626";

        StringBuilder rows = new StringBuilder();
        if (transactions.isEmpty()) {
            rows.append("<tr><td colspan=\"5\" style=\"padding:14px;text-align:center;color:#999999;\">Aucune transaction ce mois-ci</td></tr>");
        } else {
            for (Transaction t : transactions) {
                boolean revenu = t.getType() == TypeTransaction.REVENU;
                String typeLabel = revenu ? "Revenu" : "Depense";
                String color = revenu ? "#059669" : "#dc2626";
                String sign = revenu ? "+" : "-";
                rows.append("""
                    <tr>
                        <td style="padding:6px 8px;border-bottom:1px solid #eeeeee;">%s</td>
                        <td style="padding:6px 8px;border-bottom:1px solid #eeeeee;color:%s;">%s</td>
                        <td style="padding:6px 8px;border-bottom:1px solid #eeeeee;">%s</td>
                        <td style="padding:6px 8px;border-bottom:1px solid #eeeeee;">%s</td>
                        <td style="padding:6px 8px;border-bottom:1px solid #eeeeee;text-align:right;color:%s;">%s%s FCFA</td>
                    </tr>
                    """.formatted(
                        t.getDateTransaction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        color, typeLabel,
                        escape(t.getCategorie()),
                        escape(t.getDescription()),
                        color, sign, formatMontant(t.getMontant())
                ));
            }
        }

        return """
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: Helvetica, Arial, sans-serif; font-size: 11px; color: #1a1a2e; }
                    h1 { font-size: 18px; margin: 0 0 4px 0; }
                    .subtitle { color: #666666; font-size: 11px; margin-bottom: 20px; }
                    table.summary { width: 100%%; border-collapse: collapse; margin-bottom: 20px; }
                    table.summary td { padding: 12px; text-align: center; border: 1px solid #eeeeee; width: 33%%; }
                    .summary-label { color: #666666; font-size: 10px; text-transform: uppercase; }
                    .summary-value { font-size: 15px; font-weight: bold; }
                    table.detail { width: 100%%; border-collapse: collapse; }
                    table.detail thead th { background: #1a1a2e; color: #ffffff; padding: 8px; text-align: left; font-size: 10px; text-transform: uppercase; }
                    .footer { margin-top: 24px; color: #999999; font-size: 9px; text-align: center; }
                </style>
            </head>
            <body>
                <h1>%s</h1>
                <div class="subtitle">Rapport financier mensuel — %s</div>

                <table class="summary">
                    <tr>
                        <td>
                            <div class="summary-label">Revenus</div>
                            <div class="summary-value" style="color:#059669;">%s FCFA</div>
                        </td>
                        <td>
                            <div class="summary-label">Depenses</div>
                            <div class="summary-value" style="color:#dc2626;">%s FCFA</div>
                        </td>
                        <td>
                            <div class="summary-label">Solde</div>
                            <div class="summary-value" style="color:%s;">%s FCFA</div>
                        </td>
                    </tr>
                </table>

                <table class="detail">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Type</th>
                            <th>Categorie</th>
                            <th>Description</th>
                            <th>Montant</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>

                <div class="footer">Rapport genere automatiquement par MaGestionHotel</div>
            </body>
            </html>
            """.formatted(
                escape(hotelName), moisLabel,
                formatMontant(totalRevenus), formatMontant(totalDepenses),
                soldeColor, formatMontant(solde),
                rows.toString()
        );
    }

    private String buildMonthlyReportHtml(String hotelName, String moisLabel, List<Transaction> transactions,
                                           BigDecimal totalRevenus, BigDecimal totalDepenses) {
        BigDecimal solde = totalRevenus.subtract(totalDepenses);
        String soldeColor = solde.compareTo(BigDecimal.ZERO) >= 0 ? "#059669" : "#dc2626";

        StringBuilder rows = new StringBuilder();
        if (transactions.isEmpty()) {
            rows.append("<tr><td colspan=\"5\" style=\"padding:20px;text-align:center;color:#999;font-size:13px;\">Aucune transaction ce mois-ci</td></tr>");
        } else {
            for (Transaction t : transactions) {
                boolean revenu = t.getType() == TypeTransaction.REVENU;
                String typeLabel = revenu ? "Revenu" : "Dépense";
                String color = revenu ? "#059669" : "#dc2626";
                String sign = revenu ? "+" : "-";
                rows.append("""
                    <tr>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;font-size:13px;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;font-size:13px;color:%s;font-weight:600;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;font-size:13px;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;font-size:13px;">%s</td>
                        <td style="padding:8px 12px;border-bottom:1px solid #eee;font-size:13px;text-align:right;color:%s;font-weight:600;">%s%s FCFA</td>
                    </tr>
                    """.formatted(
                        t.getDateTransaction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        color, typeLabel,
                        escape(t.getCategorie()),
                        escape(t.getDescription()),
                        color, sign, formatMontant(t.getMontant())
                ));
            }
        }

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
                            <table width="680" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                                <!-- Header -->
                                <tr>
                                    <td style="background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 100%%);padding:32px 40px;text-align:center;">
                                        <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;letter-spacing:1px;">
                                            🏨 %s
                                        </h1>
                                        <p style="color:#b8c5e8;margin:8px 0 0;font-size:14px;">
                                            Rapport financier mensuel — %s
                                        </p>
                                    </td>
                                </tr>

                                <!-- Résumé -->
                                <tr>
                                    <td style="padding:32px 40px 16px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td width="33%%" style="background:#f0fdf4;border-radius:8px;padding:16px;text-align:center;">
                                                    <div style="color:#666;font-size:11px;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px;">Revenus</div>
                                                    <div style="color:#059669;font-size:18px;font-weight:800;">%s FCFA</div>
                                                </td>
                                                <td width="2%%"></td>
                                                <td width="33%%" style="background:#fef2f2;border-radius:8px;padding:16px;text-align:center;">
                                                    <div style="color:#666;font-size:11px;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px;">Dépenses</div>
                                                    <div style="color:#dc2626;font-size:18px;font-weight:800;">%s FCFA</div>
                                                </td>
                                                <td width="2%%"></td>
                                                <td width="33%%" style="background:#f8f6f0;border-radius:8px;padding:16px;text-align:center;">
                                                    <div style="color:#666;font-size:11px;text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px;">Solde</div>
                                                    <div style="color:%s;font-size:18px;font-weight:800;">%s FCFA</div>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Détail des transactions -->
                                <tr>
                                    <td style="padding:8px 40px 40px;">
                                        <h2 style="color:#1a1a2e;margin:16px 0;font-size:16px;">Détail des entrées et sorties</h2>
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #eee;border-radius:8px;overflow:hidden;">
                                            <thead>
                                                <tr style="background:#1a1a2e;">
                                                    <th style="padding:10px 12px;text-align:left;color:#fff;font-size:11px;text-transform:uppercase;">Date</th>
                                                    <th style="padding:10px 12px;text-align:left;color:#fff;font-size:11px;text-transform:uppercase;">Type</th>
                                                    <th style="padding:10px 12px;text-align:left;color:#fff;font-size:11px;text-transform:uppercase;">Catégorie</th>
                                                    <th style="padding:10px 12px;text-align:left;color:#fff;font-size:11px;text-transform:uppercase;">Description</th>
                                                    <th style="padding:10px 12px;text-align:right;color:#fff;font-size:11px;text-transform:uppercase;">Montant</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                %s
                                            </tbody>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background:#f8f8f8;padding:24px 40px;border-top:1px solid #eee;text-align:center;">
                                        <p style="color:#999;font-size:12px;margin:0;line-height:1.5;">
                                            Rapport généré automatiquement en fin de mois par MaGestionHotel.<br>
                                            &copy; 2025 MaGestionHotel — Tous droits réservés
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                escape(hotelName), moisLabel,
                formatMontant(totalRevenus), formatMontant(totalDepenses),
                soldeColor, formatMontant(solde),
                rows.toString()
        );
    }

    private String formatMontant(BigDecimal montant) {
        return NumberFormat.getInstance(Locale.FRANCE).format(montant != null ? montant : BigDecimal.ZERO);
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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