package com.mghbackend.controller;

import com.mghbackend.dto.CommandeRestaurantDto;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.enums.StatutCommandeRestaurant;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.CommandeRestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/commandes-restaurant")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommandeRestaurantController {

    private final CommandeRestaurantService commandeService;

    @PostMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_CREER_COMMANDE')")
    public ResponseEntity<ApiResponse<CommandeRestaurantDto>> createCommande(
            @Valid @RequestBody CommandeRestaurantDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            Long userId = principal.getAccountType().equals("USER") ? principal.getId() : null;
            CommandeRestaurantDto commande = commandeService.createCommande(
                    principal.getHotelId(), dto, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Commande créée avec succès", commande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMMANDES')")
    public ResponseEntity<ApiResponse<List<CommandeRestaurantDto>>> getCommandes(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<CommandeRestaurantDto> commandes = commandeService.getCommandesByHotel(
                    principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(commandes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAnyAuthority('PERMISSION_VOIR_COMMANDES')")
    public ResponseEntity<ApiResponse<CommandeRestaurantDto>> getCommande(
            @PathVariable Long id) {
        try {
            CommandeRestaurantDto commande = commandeService.getCommandeById(id);
            return ResponseEntity.ok(ApiResponse.success(commande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMMANDE')")
    public ResponseEntity<ApiResponse<CommandeRestaurantDto>> updateStatut(
            @PathVariable Long id,
            @RequestParam StatutCommandeRestaurant statut) {
        try {
            CommandeRestaurantDto commande = commandeService.updateStatut(id, statut);
            return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", commande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/paiement")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMMANDE')")
    public ResponseEntity<ApiResponse<CommandeRestaurantDto>> addPaiement(
            @PathVariable Long id,
            @RequestParam BigDecimal montant) {
        try {
            CommandeRestaurantDto commande = commandeService.addPaiement(id, montant);
            return ResponseEntity.ok(ApiResponse.success("Paiement enregistré", commande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── ✅ NOUVEAU : Appliquer une remise totale sur une commande ─────────────

    @PutMapping("/{id}/remise")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMMANDE')")
    public ResponseEntity<ApiResponse<CommandeRestaurantDto>> appliquerRemise(
            @PathVariable Long id,
            @RequestParam BigDecimal montant) {
        try {
            CommandeRestaurantDto commande = commandeService.appliquerRemise(id, montant);
            return ResponseEntity.ok(ApiResponse.success("Remise appliquée avec succès", commande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMMANDES')")
    public ResponseEntity<ApiResponse<List<CommandeRestaurantDto>>> getCommandesByReservation(
            @PathVariable Long reservationId) {
        try {
            List<CommandeRestaurantDto> commandes = commandeService.getCommandesByReservation(reservationId);
            return ResponseEntity.ok(ApiResponse.success(commandes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}