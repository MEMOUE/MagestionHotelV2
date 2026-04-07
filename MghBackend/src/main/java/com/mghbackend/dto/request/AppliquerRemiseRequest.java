package com.mghbackend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppliquerRemiseRequest {

    /** Remise par nuit (ex: chambre à 10000, remiseJournaliere=3000 → prix effectif 7000/nuit) */
    private BigDecimal remiseJournaliere;

    /** Remise globale soustraite du montant total */
    private BigDecimal remiseTotale;
}
