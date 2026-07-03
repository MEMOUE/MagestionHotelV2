package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.request.ForgotPasswordRequest;
import com.mghbackend.dto.request.ResetPasswordRequest;
import com.mghbackend.entity.Hotel;
import com.mghbackend.entity.PasswordResetToken;
import com.mghbackend.entity.User;
import com.mghbackend.repository.HotelRepository;
import com.mghbackend.repository.PasswordResetTokenRepository;
import com.mghbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Durée de validité du token : 30 minutes */
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    /**
     * Étape 1 : Demande de réinitialisation.
     * Génère un token unique et envoie un email avec le lien.
     */
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String accountType = request.getAccountType();

        // Vérifier que le compte existe
        String recipientName;
        if ("HOTEL".equals(accountType)) {
            Hotel hotel = hotelRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Aucun compte hôtel trouvé avec cet email"));
            if (!hotel.getActive()) {
                throw new BusinessException("Ce compte hôtel est désactivé");
            }
            recipientName = hotel.getName();
        } else if ("USER".equals(accountType)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Aucun compte employé trouvé avec cet email"));
            if (!user.getActive()) {
                throw new BusinessException("Ce compte employé est désactivé");
            }
            recipientName = user.getFirstName() + " " + user.getLastName();
        } else {
            throw new BusinessException("Type de compte invalide");
        }

        // Invalider les anciens tokens non utilisés
        tokenRepository.invalidateExistingTokens(email, accountType);

        // Générer un nouveau token
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setAccountType(accountType);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        // Envoyer l'email
        emailService.sendPasswordResetEmail(email, token, accountType, recipientName);

        log.info("🔐 Token de réinitialisation généré pour {} (type: {})", email, accountType);
    }

    /**
     * Étape 2 : Valide le token (appelé quand l'utilisateur ouvre le lien).
     * Retourne les infos nécessaires pour afficher le formulaire.
     */
    @Transactional(readOnly = true)
    public void validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new BusinessException("Token invalide ou déjà utilisé"));

        if (resetToken.isExpired()) {
            throw new BusinessException("Ce lien a expiré. Veuillez refaire une demande de réinitialisation.");
        }
    }

    /**
     * Étape 3 : Réinitialise le mot de passe avec le token.
     */
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BusinessException("Token invalide ou déjà utilisé"));

        if (resetToken.isExpired()) {
            throw new BusinessException("Ce lien a expiré. Veuillez refaire une demande de réinitialisation.");
        }

        String email = resetToken.getEmail();
        String accountType = resetToken.getAccountType();
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        if ("HOTEL".equals(accountType)) {
            Hotel hotel = hotelRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Compte hôtel introuvable"));
            hotel.setPassword(encodedPassword);
            hotelRepository.save(hotel);
            log.info("✅ Mot de passe réinitialisé pour l'hôtel : {}", email);

        } else if ("USER".equals(accountType)) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Compte employé introuvable"));
            user.setPassword(encodedPassword);
            userRepository.save(user);
            log.info("✅ Mot de passe réinitialisé pour l'employé : {}", email);
        }

        // Marquer le token comme utilisé
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}