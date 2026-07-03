package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.MouvementStockDto;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.ProduitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mouvements-stock")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MouvementStockController {

    private final ProduitService produitService;

    /**
     * Historique complet de l'hôtel — triés du plus récent au plus ancien.
     * GET /api/mouvements-stock
     */
    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_STOCK')")
    public ResponseEntity<ApiResponse<List<MouvementStockDto>>> getHistorique(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<MouvementStockDto> mouvements =
                    produitService.getHistoriqueByHotel(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(mouvements));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Historique d'un produit spécifique.
     * GET /api/mouvements-stock/produit/{produitId}
     */
    @GetMapping("/produit/{produitId}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_STOCK')")
    public ResponseEntity<ApiResponse<List<MouvementStockDto>>> getHistoriqueProduit(
            @PathVariable Long produitId) {
        try {
            List<MouvementStockDto> mouvements =
                    produitService.getHistoriqueByProduit(produitId);
            return ResponseEntity.ok(ApiResponse.success(mouvements));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}