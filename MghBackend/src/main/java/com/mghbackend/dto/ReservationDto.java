package com.mghbackend.dto;

import com.mghbackend.enums.ModePaiement;
import com.mghbackend.enums.StatutPaiement;
import com.mghbackend.enums.StatutReservation;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private Long id;
    private String numeroReservation;

    @NotNull(message = "La chambre est obligatoire")
    private Long chambreId;
    private String chambreNumero;

    @NotNull(message = "Le client est obligatoire")
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;

    private Long hotelId;

    @NotNull(message = "La date d'arrivée est obligatoire")
    @FutureOrPresent(message = "La date d'arrivée ne peut pas être dans le passé")
    private LocalDate dateArrivee;

    @NotNull(message = "La date de départ est obligatoire")
    @Future(message = "La date de départ doit être dans le futur")
    private LocalDate dateDepart;

    private Integer nombreNuits;

    @NotNull(message = "Le nombre d'adultes est obligatoire")
    @Min(value = 1, message = "Au moins 1 adulte requis")
    private Integer nombreAdultes;

    @Min(value = 0, message = "Le nombre d'enfants ne peut pas être négatif")
    private Integer nombreEnfants;

    private BigDecimal prixParNuit;

    // ── Remises ────────────────────────────────────────────────
    private BigDecimal remiseJournaliere;
    private BigDecimal remiseTotale;

    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;

    private StatutReservation statut;
    private StatutPaiement statutPaiement;
    private ModePaiement modePaiement;

    private String notes;
    private String demandesSpeciales;

    private LocalDateTime dateCheckin;
    private LocalDateTime dateCheckout;

    private Long createdById;
    private String createdByName;

    private Long checkinById;
    private String checkinByName;

    private Long checkoutById;
    private String checkoutByName;

    private String referenceExterne;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}