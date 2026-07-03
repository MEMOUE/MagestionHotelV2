package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.request.AppliquerRemiseRequest;
import com.mghbackend.dto.request.CreateReservationRequest;
import com.mghbackend.dto.request.UpdateReservationRequest;
import com.mghbackend.dto.ReservationDto;
import com.mghbackend.enums.StatutReservation;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.ReservationService;
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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    // ─── Création ─────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_CREER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ReservationDto reservation = reservationService.createReservation(
                    principal.getHotelId(), request,
                    principal.getAccountType().equals("USER") ? principal.getId() : null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Réservation créée avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Lecture ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationById(id)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/numero/{numero}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservationByNumero(@PathVariable String numero) {
        try {
            return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationByNumero(numero)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservations(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getReservationsByHotel(principal.getHotelId())));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByStatut(
            @PathVariable StatutReservation statut,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getReservationsByHotelAndStatut(principal.getHotelId(), statut)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByClient(
            @PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getReservationsByClient(clientId)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/arrivees-aujourdhui")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getArriveesAujourdhui(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getArrivalsForToday(principal.getHotelId())));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/departs-aujourdhui")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getDepartsAujourdhui(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getDeparturesForToday(principal.getHotelId())));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/en-cours")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsEnCours(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getReservationsEnCours(principal.getHotelId())));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/a-venir")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsAVenir(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.getReservationsAVenir(principal.getHotelId())));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> searchReservations(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    reservationService.searchReservations(principal.getHotelId(), keyword)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── ✅ NOUVEAU : Modification complète (dates, voyageurs, notes) ──────────

    /**
     * Modifie une réservation existante : dates (prolongation/réduction), voyageurs, notes.
     * Le backend vérifie la disponibilité de la chambre pour les nouvelles dates
     * en excluant la réservation courante du check de conflit.
     */
    @PutMapping("/{id}/modifier")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> modifierReservation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationRequest request) {
        try {
            ReservationDto reservation = reservationService.modifierReservation(id, request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Réservation modifiée avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> doCheckin(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ReservationDto reservation = reservationService.doCheckin(
                    id, principal.getAccountType().equals("USER") ? principal.getId() : null);
            return ResponseEntity.ok(ApiResponse.success("Check-in effectué avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> doCheckout(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ReservationDto reservation = reservationService.doCheckout(
                    id, principal.getAccountType().equals("USER") ? principal.getId() : null);
            return ResponseEntity.ok(ApiResponse.success("Check-out effectué avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_ANNULER_RESERVATION')")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        try {
            reservationService.cancelReservation(id);
            return ResponseEntity.ok(ApiResponse.success("Réservation annulée avec succès", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/paiement")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> addPaiement(
            @PathVariable Long id,
            @RequestParam BigDecimal montant,
            @RequestParam String modePaiement) {
        try {
            ReservationDto reservation = reservationService.addPaiement(id, montant, modePaiement);
            return ResponseEntity.ok(ApiResponse.success("Paiement enregistré avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/remise")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ReservationDto>> appliquerRemise(
            @PathVariable Long id,
            @RequestBody AppliquerRemiseRequest request) {
        try {
            ReservationDto reservation = reservationService.appliquerRemise(id, request);
            return ResponseEntity.ok(ApiResponse.success("Remise appliquée avec succès", reservation));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
