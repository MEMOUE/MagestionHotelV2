package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.request.ForgotPasswordRequest;
import com.mghbackend.dto.request.LoginRequest;
import com.mghbackend.dto.reponse.LoginResponse;
import com.mghbackend.dto.request.ResetPasswordRequest;
import com.mghbackend.service.AuthService;
import com.mghbackend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Étape 1 : Demande de réinitialisation de mot de passe.
     * Envoie un email avec un lien contenant un token unique.
     *
     * POST /api/auth/forgot-password
     * Body: { "email": "...", "accountType": "HOTEL" | "USER" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.requestPasswordReset(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Un email de réinitialisation a été envoyé à " + request.getEmail(), null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Étape 2 : Validation du token (quand l'utilisateur clique sur le lien).
     * Vérifie que le token est valide et non expiré.
     *
     * GET /api/auth/validate-token?token=xxx
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<Void>> validateToken(@RequestParam String token) {
        try {
            passwordResetService.validateToken(token);
            return ResponseEntity.ok(ApiResponse.success("Token valide", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Étape 3 : Réinitialisation du mot de passe.
     *
     * POST /api/auth/reset-password
     * Body: { "token": "...", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}