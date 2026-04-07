package com.mghbackend.dto;

import com.mghbackend.enums.StatutCommandeRestaurant;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeRestaurantDto {
    private Long id;
    private String numeroCommande;

    private String nomClientExterne;
    private String telephoneClientExterne;

    private Long clientId;
    private String clientNom;

    private Long reservationId;
    private String reservationNumero;

    private String numeroTable;
    private StatutCommandeRestaurant statut;

    @NotNull(message = "Le montant total est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant doit être positif")
    private BigDecimal montantTotal;

    /** Remise globale soustraite du montant brut */
    private BigDecimal remiseTotale;

    private BigDecimal montantPaye;
    private String notes;
    private LocalDateTime dateCommande;
    private LocalDateTime dateService;

    private Long serveurId;
    private String serveurNom;

    private Long hotelId;

    @NotEmpty(message = "La commande doit contenir au moins un produit")
    private List<LigneCommandeDto> lignes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}