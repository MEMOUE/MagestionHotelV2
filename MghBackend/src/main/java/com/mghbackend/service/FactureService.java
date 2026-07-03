package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.FactureDto;
import com.mghbackend.dto.LigneFactureDto;
import com.mghbackend.entity.*;
import com.mghbackend.enums.StatutFacture;
import com.mghbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FactureService {

    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;
    private final ReservationRepository reservationRepository;
    private final CommandeRestaurantRepository commandeRestaurantRepository;
    private final HotelRepository hotelRepository;
    private final LigneFactureRepository ligneFactureRepository;

    public FactureDto createFacture(Long hotelId, FactureDto dto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        Facture facture = new Facture();
        facture.setHotel(hotel);
        facture.setNumeroFacture(generateNumeroFacture());
        facture.setDateEmission(dto.getDateEmission());
        facture.setDateEcheance(dto.getDateEcheance());
        facture.setNotes(dto.getNotes());

        // Lier à client, réservation ou commande restaurant
        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new BusinessException("Client non trouvé"));
            facture.setClient(client);
        }

        if (dto.getReservationId() != null) {
            Reservation reservation = reservationRepository.findById(dto.getReservationId())
                    .orElseThrow(() -> new BusinessException("Réservation non trouvée"));
            facture.setReservation(reservation);
        }

        if (dto.getCommandeRestaurantId() != null) {
            CommandeRestaurant commande = commandeRestaurantRepository.findById(dto.getCommandeRestaurantId())
                    .orElseThrow(() -> new BusinessException("Commande restaurant non trouvée"));
            facture.setCommandeRestaurant(commande);
        }

        // Calculer les montants
        BigDecimal montantHT = BigDecimal.ZERO;
        for (LigneFactureDto ligneDto : dto.getLignes()) {
            BigDecimal montantLigneHT = ligneDto.getPrixUnitaire()
                    .multiply(BigDecimal.valueOf(ligneDto.getQuantite()));
            montantHT = montantHT.add(montantLigneHT);

            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(facture);
            ligne.setDesignation(ligneDto.getDesignation());
            ligne.setDescription(ligneDto.getDescription());
            ligne.setQuantite(ligneDto.getQuantite());
            ligne.setPrixUnitaire(ligneDto.getPrixUnitaire());
            ligne.setMontantHT(montantLigneHT);

            facture.getLignes().add(ligne);
        }

        facture.setMontantHT(montantHT);
        facture.setTauxTVA(dto.getTauxTVA() != null ? dto.getTauxTVA() : BigDecimal.ZERO);

        BigDecimal montantTVA = montantHT.multiply(facture.getTauxTVA())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        facture.setMontantTVA(montantTVA);

        BigDecimal montantTTC = montantHT.add(montantTVA);
        facture.setMontantTTC(montantTTC);
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setMontantRestant(montantTTC);
        facture.setStatut(StatutFacture.BROUILLON);

        Facture saved = factureRepository.save(facture);
        return convertToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FactureDto> getFacturesByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return factureRepository.findByHotel(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public FactureDto emettre(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new BusinessException("Facture non trouvée"));
        facture.setStatut(StatutFacture.EMISE);
        Facture saved = factureRepository.save(facture);
        return convertToDto(saved);
    }

    public FactureDto addPaiement(Long factureId, BigDecimal montant) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new BusinessException("Facture non trouvée"));

        BigDecimal nouveauMontantPaye = facture.getMontantPaye().add(montant);
        facture.setMontantPaye(nouveauMontantPaye);
        facture.setMontantRestant(facture.getMontantTTC().subtract(nouveauMontantPaye));

        if (facture.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0) {
            facture.setStatut(StatutFacture.PAYEE);
        }

        Facture saved = factureRepository.save(facture);
        return convertToDto(saved);
    }

    private String generateNumeroFacture() {
        String numero;
        do {
            numero = "FAC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (factureRepository.findByNumeroFacture(numero).isPresent());
        return numero;
    }

    private FactureDto convertToDto(Facture facture) {
        FactureDto dto = new FactureDto();
        dto.setId(facture.getId());
        dto.setNumeroFacture(facture.getNumeroFacture());
        dto.setDateEmission(facture.getDateEmission());
        dto.setDateEcheance(facture.getDateEcheance());
        dto.setStatut(facture.getStatut());

        if (facture.getClient() != null) {
            dto.setClientId(facture.getClient().getId());
            dto.setClientNom(facture.getClient().getNom() + " " + facture.getClient().getPrenom());
        }

        if (facture.getReservation() != null) {
            dto.setReservationId(facture.getReservation().getId());
            dto.setReservationNumero(facture.getReservation().getNumeroReservation());
        }

        if (facture.getCommandeRestaurant() != null) {
            dto.setCommandeRestaurantId(facture.getCommandeRestaurant().getId());
            dto.setCommandeRestaurantNumero(facture.getCommandeRestaurant().getNumeroCommande());
        }

        dto.setMontantHT(facture.getMontantHT());
        dto.setTauxTVA(facture.getTauxTVA());
        dto.setMontantTVA(facture.getMontantTVA());
        dto.setMontantTTC(facture.getMontantTTC());
        dto.setMontantPaye(facture.getMontantPaye());
        dto.setMontantRestant(facture.getMontantRestant());
        dto.setNotes(facture.getNotes());
        dto.setHotelId(facture.getHotel().getId());

        // Convertir les lignes
        List<LigneFactureDto> lignesDto = facture.getLignes().stream()
                .map(ligne -> {
                    LigneFactureDto ligneDto = new LigneFactureDto();
                    ligneDto.setId(ligne.getId());
                    ligneDto.setDesignation(ligne.getDesignation());
                    ligneDto.setDescription(ligne.getDescription());
                    ligneDto.setQuantite(ligne.getQuantite());
                    ligneDto.setPrixUnitaire(ligne.getPrixUnitaire());
                    ligneDto.setMontantHT(ligne.getMontantHT());
                    return ligneDto;
                })
                .collect(Collectors.toList());
        dto.setLignes(lignesDto);

        dto.setCreatedAt(facture.getCreatedAt());
        dto.setUpdatedAt(facture.getUpdatedAt());
        return dto;
    }


    /**
     * Génère automatiquement une facture consolidée pour une réservation :
     *  - Section HÉBERGEMENT  : X nuits × prix/nuit
     *  - Section RESTAURANT   : détail de chaque produit consommé
     *  - Sous-total par section, puis total général avec TVA
     */
    public FactureDto genererFactureReservation(Long hotelId, Long reservationId, BigDecimal tauxTVA) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (!reservation.getHotel().getId().equals(hotelId)) {
            throw new BusinessException("Cette réservation n'appartient pas à cet hôtel");
        }

        // Vérifier qu'il n'y a pas déjà une facture pour cette réservation
        List<Facture> facturesExistantes = factureRepository.findByReservation(reservation);
        if (!facturesExistantes.isEmpty()) {
            throw new BusinessException("Une facture existe déjà pour cette réservation : "
                    + facturesExistantes.get(0).getNumeroFacture());
        }

        Facture facture = new Facture();
        facture.setHotel(hotel);
        facture.setNumeroFacture(generateNumeroFacture());
        facture.setDateEmission(java.time.LocalDate.now());
        facture.setDateEcheance(java.time.LocalDate.now().plusDays(30));
        facture.setReservation(reservation);
        facture.setClient(reservation.getClient());

        BigDecimal montantHTTotal = BigDecimal.ZERO;

        // ═══════════════════════════════════════════════════════════
        // SECTION 1 : HÉBERGEMENT
        // ═══════════════════════════════════════════════════════════

        BigDecimal prixNuit = reservation.getPrixParNuit();
        int nombreNuits = reservation.getNombreNuits();
        BigDecimal montantHebergement = prixNuit.multiply(BigDecimal.valueOf(nombreNuits));

        LigneFacture ligneHebergement = new LigneFacture();
        ligneHebergement.setFacture(facture);
        ligneHebergement.setDesignation("Hébergement — Chambre " + reservation.getChambre().getNumero());
        ligneHebergement.setDescription(nombreNuits + " nuit(s) du "
                + reservation.getDateArrivee() + " au " + reservation.getDateDepart());
        ligneHebergement.setQuantite(nombreNuits);
        ligneHebergement.setPrixUnitaire(prixNuit);
        ligneHebergement.setMontantHT(montantHebergement);
        facture.getLignes().add(ligneHebergement);
        montantHTTotal = montantHTTotal.add(montantHebergement);

        // ═══════════════════════════════════════════════════════════
        // SECTION 2 : CONSOMMATION RESTAURANT
        // ═══════════════════════════════════════════════════════════

        List<CommandeRestaurant> commandesRestaurant = commandeRestaurantRepository
                .findByReservation(reservation).stream()
                .filter(c -> c.getStatut() != com.mghbackend.enums.StatutCommandeRestaurant.ANNULEE)
                .collect(java.util.stream.Collectors.toList());

        if (!commandesRestaurant.isEmpty()) {

            // Ligne séparateur / titre de section
            LigneFacture ligneSeparateur = new LigneFacture();
            ligneSeparateur.setFacture(facture);
            ligneSeparateur.setDesignation("══ Consommation Restaurant ══");
            ligneSeparateur.setDescription("Détail des commandes liées à la réservation");
            ligneSeparateur.setQuantite(1);
            ligneSeparateur.setPrixUnitaire(BigDecimal.ZERO);
            ligneSeparateur.setMontantHT(BigDecimal.ZERO);
            facture.getLignes().add(ligneSeparateur);

            BigDecimal sousotalRestaurant = BigDecimal.ZERO;

            for (CommandeRestaurant commande : commandesRestaurant) {
                for (LigneCommande ligneCmd : commande.getLignes()) {
                    BigDecimal montantLigne = ligneCmd.getPrixUnitaire()
                            .multiply(BigDecimal.valueOf(ligneCmd.getQuantite()));

                    LigneFacture ligneRestaurant = new LigneFacture();
                    ligneRestaurant.setFacture(facture);
                    ligneRestaurant.setDesignation(ligneCmd.getProduit().getNom());
                    ligneRestaurant.setDescription("Commande " + commande.getNumeroCommande()
                            + (ligneCmd.getNotes() != null ? " — " + ligneCmd.getNotes() : ""));
                    ligneRestaurant.setQuantite(ligneCmd.getQuantite());
                    ligneRestaurant.setPrixUnitaire(ligneCmd.getPrixUnitaire());
                    ligneRestaurant.setMontantHT(montantLigne);
                    facture.getLignes().add(ligneRestaurant);

                    sousotalRestaurant = sousotalRestaurant.add(montantLigne);
                }
            }

            // Ligne sous-total restaurant
            LigneFacture ligneSousTotalResto = new LigneFacture();
            ligneSousTotalResto.setFacture(facture);
            ligneSousTotalResto.setDesignation("Sous-total Restaurant");
            ligneSousTotalResto.setDescription(commandesRestaurant.size() + " commande(s)");
            ligneSousTotalResto.setQuantite(1);
            ligneSousTotalResto.setPrixUnitaire(sousotalRestaurant);
            ligneSousTotalResto.setMontantHT(sousotalRestaurant);
            facture.getLignes().add(ligneSousTotalResto);

            montantHTTotal = montantHTTotal.add(sousotalRestaurant);
        }

        // ═══════════════════════════════════════════════════════════
        // CALCULS FINAUX : TVA + TTC
        // ═══════════════════════════════════════════════════════════

        facture.setMontantHT(montantHTTotal);
        facture.setTauxTVA(tauxTVA != null ? tauxTVA : BigDecimal.ZERO);

        BigDecimal montantTVA = montantHTTotal.multiply(facture.getTauxTVA())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        facture.setMontantTVA(montantTVA);

        BigDecimal montantTTC = montantHTTotal.add(montantTVA);
        facture.setMontantTTC(montantTTC);

        // Montant déjà payé = ce qui a été payé sur la réservation
        BigDecimal dejaPaye = reservation.getMontantPaye() != null
                ? reservation.getMontantPaye() : BigDecimal.ZERO;
        facture.setMontantPaye(dejaPaye);
        facture.setMontantRestant(montantTTC.subtract(dejaPaye).max(BigDecimal.ZERO));

        facture.setStatut(com.mghbackend.enums.StatutFacture.EMISE);
        facture.setNotes("Facture générée automatiquement — Réservation "
                + reservation.getNumeroReservation());

        Facture saved = factureRepository.save(facture);

        log.info("📄 Facture {} générée pour réservation {} — HT={}, TVA={}, TTC={}, Payé={}, Restant={}",
                saved.getNumeroFacture(),
                reservation.getNumeroReservation(),
                montantHTTotal, montantTVA, montantTTC, dejaPaye, facture.getMontantRestant());

        return convertToDto(saved);
    }
}