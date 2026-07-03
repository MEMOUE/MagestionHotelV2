package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.ChambreDto;
import com.mghbackend.dto.request.DisponibiliteChambreRequest;
import com.mghbackend.enums.StatutChambre;
import com.mghbackend.enums.TypeChambre;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.ChambreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chambres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ChambreController {

    private final ChambreService chambreService;

    /**
     * ✅ CORRECTION PRINCIPALE : Ajout de @AuthenticationPrincipal pour récupérer l'utilisateur
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_CONFIGURATION')")
    public ResponseEntity<ApiResponse<ChambreDto>> createChambre(
            @Valid @RequestBody ChambreDto chambreDto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {  // ✅ AJOUT DU PARAMÈTRE

        try {
            log.info("🏨 Création chambre - User: {}, HotelId: {}",
                    principal.getEmail(), principal.getHotelId());

            // ✅ Le hotelId est récupéré automatiquement depuis le principal
            ChambreDto chambre = chambreService.createChambre(principal.getHotelId(), chambreDto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Chambre créée avec succès", chambre));
        } catch (BusinessException e) {
            log.error("❌ Erreur création chambre: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ChambreDto>> getChambre(@PathVariable Long id) {
        try {
            ChambreDto chambre = chambreService.getChambreById(id);
            return ResponseEntity.ok(ApiResponse.success(chambre));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ChambreDto>>> getChambres(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            log.info("📋 Liste chambres - HotelId: {}", principal.getHotelId());
            List<ChambreDto> chambres = chambreService.getChambresByHotel(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(chambres));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ChambreDto>>> getChambresByStatut(
            @PathVariable StatutChambre statut,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ChambreDto> chambres = chambreService.getChambresByHotelAndStatut(
                    principal.getHotelId(), statut);
            return ResponseEntity.ok(ApiResponse.success(chambres));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ChambreDto>>> getChambresByType(
            @PathVariable TypeChambre type,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ChambreDto> chambres = chambreService.getChambresByHotelAndType(
                    principal.getHotelId(), type);
            return ResponseEntity.ok(ApiResponse.success(chambres));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ChambreDto>>> searchChambres(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ChambreDto> chambres = chambreService.searchChambres(
                    principal.getHotelId(), keyword);
            return ResponseEntity.ok(ApiResponse.success(chambres));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/disponibilite")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ChambreDto>>> getChambresDisponibles(
            @Valid @RequestBody DisponibiliteChambreRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ChambreDto> chambres = chambreService.getChambresDisponibles(
                    principal.getHotelId(), request);
            return ResponseEntity.ok(ApiResponse.success(chambres));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_CONFIGURATION')")
    public ResponseEntity<ApiResponse<ChambreDto>> updateChambre(
            @PathVariable Long id,
            @Valid @RequestBody ChambreDto chambreDto) {
        try {
            ChambreDto chambre = chambreService.updateChambre(id, chambreDto);
            return ResponseEntity.ok(ApiResponse.success("Chambre mise à jour avec succès", chambre));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<Void>> updateStatut(
            @PathVariable Long id,
            @RequestParam StatutChambre statut) {
        try {
            chambreService.updateStatut(id, statut);
            return ResponseEntity.ok(ApiResponse.success("Statut de la chambre mis à jour", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL')")
    public ResponseEntity<ApiResponse<Void>> deleteChambre(@PathVariable Long id) {
        try {
            chambreService.deleteChambre(id);
            return ResponseEntity.ok(ApiResponse.success("Chambre supprimée avec succès", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}