package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.FactureDto;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.FactureService;
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
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FactureController {

    private final FactureService factureService;

    @PostMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<FactureDto>> createFacture(
            @Valid @RequestBody FactureDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            FactureDto facture = factureService.createFacture(principal.getHotelId(), dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Facture créée avec succès", facture));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<List<FactureDto>>> getFactures(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<FactureDto> factures = factureService.getFacturesByHotel(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(factures));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/emettre")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<FactureDto>> emettre(@PathVariable Long id) {
        try {
            FactureDto facture = factureService.emettre(id);
            return ResponseEntity.ok(ApiResponse.success("Facture émise", facture));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/paiement")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<FactureDto>> addPaiement(
            @PathVariable Long id,
            @RequestParam BigDecimal montant) {
        try {
            FactureDto facture = factureService.addPaiement(id, montant);
            return ResponseEntity.ok(ApiResponse.success("Paiement enregistré", facture));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }


    /**
     * Génère automatiquement une facture consolidée pour une réservation.
     * Inclut : hébergement + détail de toutes les commandes restaurant.
     *
     * POST /api/factures/reservation/{reservationId}?tauxTVA=18
     */
    @PostMapping("/reservation/{reservationId}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<FactureDto>> genererFactureReservation(
            @PathVariable Long reservationId,
            @RequestParam(defaultValue = "0") BigDecimal tauxTVA,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            FactureDto facture = factureService.genererFactureReservation(
                    principal.getHotelId(), reservationId, tauxTVA);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Facture consolidée générée avec succès", facture));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}