package com.mghbackend.entity;

import com.mghbackend.enums.ModePaiement;
import com.mghbackend.enums.StatutPaiement;
import com.mghbackend.enums.StatutReservation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@EqualsAndHashCode(callSuper = true)
public class Reservation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String numeroReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private LocalDate dateArrivee;

    @Column(nullable = false)
    private LocalDate dateDepart;

    @Column(nullable = false)
    private Integer nombreNuits;

    @Column(nullable = false)
    private Integer nombreAdultes;

    @Column(nullable = false)
    private Integer nombreEnfants = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixParNuit;

    // ── Remises ────────────────────────────────────────────────
    /** Remise par nuit (ex: chambre à 10000 → remiseJournaliere=3000 → prix effectif 7000/nuit) */
    @Column(precision = 10, scale = 2)
    private BigDecimal remiseJournaliere = BigDecimal.ZERO;

    /** Remise globale soustraite du montant total */
    @Column(precision = 10, scale = 2)
    private BigDecimal remiseTotale = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantRestant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement = StatutPaiement.NON_PAYE;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String demandesSpeciales;

    private LocalDateTime dateCheckin;
    private LocalDateTime dateCheckout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_by")
    private User checkinBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_by")
    private User checkoutBy;

    @Column(length = 100)
    private String referenceExterne;
}