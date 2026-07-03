package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.ClientDto;
import com.mghbackend.dto.request.AppliquerRemiseRequest;
import com.mghbackend.dto.request.CreateReservationRequest;
import com.mghbackend.dto.request.UpdateReservationRequest;
import com.mghbackend.dto.ReservationDto;
import com.mghbackend.entity.*;
import com.mghbackend.enums.*;
import com.mghbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChambreRepository chambreRepository;
    private final ClientRepository clientRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final ClientService clientService;
    private final TransactionHelper transactionHelper;

    // ─── Création ─────────────────────────────────────────────────────────────

    public ReservationDto createReservation(Long hotelId, CreateReservationRequest request, Long userId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        Chambre chambre = chambreRepository.findById(request.getChambreId())
                .orElseThrow(() -> new BusinessException("Chambre non trouvée"));

        if (!chambre.getHotel().getId().equals(hotelId)) {
            throw new BusinessException("Cette chambre n'appartient pas à cet hôtel");
        }

        if (request.getDateArrivee().isAfter(request.getDateDepart())) {
            throw new BusinessException("La date d'arrivée doit être avant la date de départ");
        }

        List<Reservation> conflits = reservationRepository.findReservationsByChambreAndPeriode(
                chambre, request.getDateArrivee(), request.getDateDepart());

        if (!conflits.isEmpty()) {
            throw new BusinessException("La chambre n'est pas disponible pour cette période");
        }

        Client client;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new BusinessException("Client non trouvé"));
        } else if (request.getNewClient() != null) {
            ClientDto clientDto = clientService.createClient(hotelId, request.getNewClient());
            client = clientRepository.findById(clientDto.getId())
                    .orElseThrow(() -> new BusinessException("Erreur lors de la création du client"));
        } else {
            throw new BusinessException("Client requis");
        }

        long nombreNuits = ChronoUnit.DAYS.between(request.getDateArrivee(), request.getDateDepart());
        BigDecimal montantTotal = chambre.getPrixParNuit().multiply(BigDecimal.valueOf(nombreNuits));

        Reservation reservation = new Reservation();
        reservation.setNumeroReservation(generateNumeroReservation());
        reservation.setHotel(hotel);
        reservation.setChambre(chambre);
        reservation.setClient(client);
        reservation.setDateArrivee(request.getDateArrivee());
        reservation.setDateDepart(request.getDateDepart());
        reservation.setNombreNuits((int) nombreNuits);
        reservation.setNombreAdultes(request.getNombreAdultes());
        reservation.setNombreEnfants(request.getNombreEnfants() != null ? request.getNombreEnfants() : 0);
        reservation.setPrixParNuit(chambre.getPrixParNuit());
        reservation.setRemiseJournaliere(BigDecimal.ZERO);
        reservation.setRemiseTotale(BigDecimal.ZERO);
        reservation.setMontantTotal(montantTotal);
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservation.setNotes(request.getNotes());
        reservation.setDemandesSpeciales(request.getDemandesSpeciales());
        reservation.setReferenceExterne(request.getReferenceExterne());

        if (request.getMontantPaye() != null && request.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setMontantPaye(request.getMontantPaye());
            reservation.setMontantRestant(montantTotal.subtract(request.getMontantPaye()));
            if (request.getModePaiement() != null) {
                reservation.setModePaiement(ModePaiement.valueOf(request.getModePaiement()));
            }
            reservation.setStatutPaiement(
                    reservation.getMontantRestant().compareTo(BigDecimal.ZERO) <= 0
                            ? StatutPaiement.PAYE
                            : StatutPaiement.ACOMPTE
            );
        } else {
            reservation.setMontantPaye(BigDecimal.ZERO);
            reservation.setMontantRestant(montantTotal);
            reservation.setStatutPaiement(StatutPaiement.NON_PAYE);
        }

        if (userId != null) {
            userRepository.findById(userId).ifPresent(reservation::setCreatedBy);
        }

        chambre.setStatut(request.getDateArrivee().equals(LocalDate.now())
                ? StatutChambre.OCCUPEE : StatutChambre.RESERVEE);
        chambreRepository.save(chambre);

        Reservation saved = reservationRepository.save(reservation);

        if (request.getMontantPaye() != null && request.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
            transactionHelper.enregistrerPaiementReservation(
                    hotelId, saved.getId(), saved.getNumeroReservation(),
                    client.getNom() + " " + client.getPrenom(),
                    request.getMontantPaye(), request.getModePaiement());
        }
        return convertToDto(saved);
    }

    // ─── Appliquer une remise ─────────────────────────────────────────────────

    public ReservationDto appliquerRemise(Long id, AppliquerRemiseRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Impossible d'appliquer une remise sur une réservation annulée");
        }
        if (reservation.getStatut() == StatutReservation.TERMINEE) {
            throw new BusinessException("Impossible d'appliquer une remise sur une réservation terminée");
        }

        BigDecimal remiseJourn = request.getRemiseJournaliere() != null
                ? request.getRemiseJournaliere() : reservation.getRemiseJournaliere();
        BigDecimal remiseTotal = request.getRemiseTotale() != null
                ? request.getRemiseTotale() : reservation.getRemiseTotale();

        // Validation : la remise journalière ne doit pas dépasser le prix par nuit
        if (remiseJourn.compareTo(reservation.getPrixParNuit()) > 0) {
            throw new BusinessException("La remise journalière (" + remiseJourn +
                    ") ne peut pas dépasser le prix par nuit (" + reservation.getPrixParNuit() + ")");
        }
        if (remiseJourn.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La remise journalière ne peut pas être négative");
        }
        if (remiseTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La remise totale ne peut pas être négative");
        }

        reservation.setRemiseJournaliere(remiseJourn);
        reservation.setRemiseTotale(remiseTotal);

        // Recalcul du montant total
        recalculerMontants(reservation);

        Reservation saved = reservationRepository.save(reservation);

        log.info("💸 Remise appliquée sur réservation {} : journalière={}, totale={}, nouveau total={}",
                saved.getNumeroReservation(), remiseJourn, remiseTotal, saved.getMontantTotal());

        return convertToDto(saved);
    }

    /**
     * Recalcule montantTotal, montantRestant et statutPaiement
     * en tenant compte des remises.
     */
    private void recalculerMontants(Reservation reservation) {
        BigDecimal prixEffectif = reservation.getPrixParNuit()
                .subtract(reservation.getRemiseJournaliere() != null
                        ? reservation.getRemiseJournaliere() : BigDecimal.ZERO);

        BigDecimal montantBrut = prixEffectif.multiply(BigDecimal.valueOf(reservation.getNombreNuits()));

        BigDecimal remiseTotale = reservation.getRemiseTotale() != null
                ? reservation.getRemiseTotale() : BigDecimal.ZERO;

        BigDecimal montantTotal = montantBrut.subtract(remiseTotale);
        if (montantTotal.compareTo(BigDecimal.ZERO) < 0) {
            montantTotal = BigDecimal.ZERO;
        }

        reservation.setMontantTotal(montantTotal);

        BigDecimal montantPaye = reservation.getMontantPaye() != null
                ? reservation.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal montantRestant = montantTotal.subtract(montantPaye);
        if (montantRestant.compareTo(BigDecimal.ZERO) < 0) {
            montantRestant = BigDecimal.ZERO;
        }
        reservation.setMontantRestant(montantRestant);

        // Recalcul statut paiement
        if (montantRestant.compareTo(BigDecimal.ZERO) <= 0) {
            reservation.setStatutPaiement(StatutPaiement.PAYE);
        } else if (montantPaye.compareTo(BigDecimal.ZERO) > 0) {
            reservation.setStatutPaiement(StatutPaiement.ACOMPTE);
        } else {
            reservation.setStatutPaiement(StatutPaiement.NON_PAYE);
        }
    }

    // ─── Modification complète ──────────────────────────────────────────────

    public ReservationDto modifierReservation(Long id, UpdateReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Impossible de modifier une réservation annulée");
        }
        if (reservation.getStatut() == StatutReservation.TERMINEE) {
            throw new BusinessException("Impossible de modifier une réservation terminée");
        }

        if (request.getDateArrivee().isAfter(request.getDateDepart())) {
            throw new BusinessException("La date d'arrivée doit être avant la date de départ");
        }

        if (reservation.getStatut() == StatutReservation.EN_COURS) {
            if (!request.getDateArrivee().equals(reservation.getDateArrivee())) {
                throw new BusinessException(
                        "Le client est déjà arrivé : la date d'arrivée ne peut plus être modifiée.");
            }
        }

        boolean datesChanged = !request.getDateArrivee().equals(reservation.getDateArrivee())
                || !request.getDateDepart().equals(reservation.getDateDepart());

        if (datesChanged) {
            List<Reservation> conflits = reservationRepository
                    .findReservationsByChambreAndPeriodeExcluding(
                            reservation.getChambre(),
                            request.getDateArrivee(),
                            request.getDateDepart(),
                            id
                    );
            if (!conflits.isEmpty()) {
                throw new BusinessException("La chambre n'est pas disponible pour ces nouvelles dates.");
            }
        }

        long nouveauxNuits = ChronoUnit.DAYS.between(request.getDateArrivee(), request.getDateDepart());

        reservation.setDateArrivee(request.getDateArrivee());
        reservation.setDateDepart(request.getDateDepart());
        reservation.setNombreNuits((int) nouveauxNuits);
        reservation.setNombreAdultes(request.getNombreAdultes());
        reservation.setNombreEnfants(request.getNombreEnfants() != null ? request.getNombreEnfants() : 0);
        reservation.setNotes(request.getNotes());
        reservation.setDemandesSpeciales(request.getDemandesSpeciales());
        reservation.setReferenceExterne(request.getReferenceExterne());

        // Recalcul avec les remises existantes
        recalculerMontants(reservation);

        return convertToDto(reservationRepository.save(reservation));
    }

    // ─── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReservationDto getReservationById(Long id) {
        return convertToDto(reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée")));
    }

    @Transactional(readOnly = true)
    public ReservationDto getReservationByNumero(String numeroReservation) {
        return convertToDto(reservationRepository.findByNumeroReservation(numeroReservation)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée")));
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getReservationsByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findByHotel(hotel).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getReservationsByHotelAndStatut(Long hotelId, StatutReservation statut) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findByHotelAndStatut(hotel, statut).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getReservationsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));
        return reservationRepository.findByClient(client).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getArrivalsForToday(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findArrivalsForToday(hotel, LocalDate.now()).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getDeparturesForToday(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findDeparturesForToday(hotel, LocalDate.now()).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getReservationsEnCours(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findReservationsEnCours(hotel).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getReservationsAVenir(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.findReservationsAVenir(hotel, LocalDate.now()).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> searchReservations(Long hotelId, String keyword) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return reservationRepository.searchByHotelAndKeyword(hotel, keyword).stream()
                .map(this::convertToDto).collect(Collectors.toList());
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    public ReservationDto doCheckin(Long id, Long userId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new BusinessException("Seules les réservations confirmées peuvent faire l'objet d'un check-in");
        }

        reservation.setStatut(StatutReservation.EN_COURS);
        reservation.setDateCheckin(java.time.LocalDateTime.now());
        if (userId != null) userRepository.findById(userId).ifPresent(reservation::setCheckinBy);

        Chambre chambre = reservation.getChambre();
        chambre.setStatut(StatutChambre.OCCUPEE);
        chambreRepository.save(chambre);

        return convertToDto(reservationRepository.save(reservation));
    }

    public ReservationDto doCheckout(Long id, Long userId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (reservation.getStatut() != StatutReservation.EN_COURS) {
            throw new BusinessException("Seules les réservations en cours peuvent faire l'objet d'un check-out");
        }

        reservation.setStatut(StatutReservation.TERMINEE);
        reservation.setDateCheckout(java.time.LocalDateTime.now());
        if (userId != null) userRepository.findById(userId).ifPresent(reservation::setCheckoutBy);

        Chambre chambre = reservation.getChambre();
        chambre.setStatut(StatutChambre.EN_NETTOYAGE);
        chambreRepository.save(chambre);

        return convertToDto(reservationRepository.save(reservation));
    }

    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (reservation.getStatut() == StatutReservation.TERMINEE) {
            throw new BusinessException("Impossible d'annuler une réservation terminée");
        }
        if (reservation.getStatut() == StatutReservation.EN_COURS) {
            throw new BusinessException("Impossible d'annuler une réservation en cours. Effectuez d'abord un check-out.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        Chambre chambre = reservation.getChambre();
        chambre.setStatut(StatutChambre.DISPONIBLE);
        chambreRepository.save(chambre);
        reservationRepository.save(reservation);
    }

    public ReservationDto addPaiement(Long id, BigDecimal montant, String modePaiement) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Réservation non trouvée"));

        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant doit être supérieur à 0");
        }

        BigDecimal nouveauMontantPaye = reservation.getMontantPaye().add(montant);
        BigDecimal nouveauMontantRestant = reservation.getMontantTotal().subtract(nouveauMontantPaye);

        if (nouveauMontantRestant.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Le montant payé dépasse le montant total");
        }

        reservation.setMontantPaye(nouveauMontantPaye);
        reservation.setMontantRestant(nouveauMontantRestant);
        if (modePaiement != null) reservation.setModePaiement(ModePaiement.valueOf(modePaiement));
        reservation.setStatutPaiement(nouveauMontantRestant.compareTo(BigDecimal.ZERO) <= 0
                ? StatutPaiement.PAYE : StatutPaiement.ACOMPTE);

        Reservation updated = reservationRepository.save(reservation);
        transactionHelper.enregistrerPaiementReservation(
                reservation.getHotel().getId(), reservation.getId(),
                reservation.getNumeroReservation(),
                reservation.getClient().getNom() + " " + reservation.getClient().getPrenom(),
                montant, modePaiement);
        return convertToDto(updated);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateNumeroReservation() {
        String numero;
        do {
            numero = "RES" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (reservationRepository.existsByNumeroReservation(numero));
        return numero;
    }

    private ReservationDto convertToDto(Reservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.setId(reservation.getId());
        dto.setNumeroReservation(reservation.getNumeroReservation());
        dto.setChambreId(reservation.getChambre().getId());
        dto.setChambreNumero(reservation.getChambre().getNumero());
        dto.setClientId(reservation.getClient().getId());
        dto.setClientNom(reservation.getClient().getNom());
        dto.setClientPrenom(reservation.getClient().getPrenom());
        dto.setClientTelephone(reservation.getClient().getTelephone());
        dto.setHotelId(reservation.getHotel().getId());
        dto.setDateArrivee(reservation.getDateArrivee());
        dto.setDateDepart(reservation.getDateDepart());
        dto.setNombreNuits(reservation.getNombreNuits());
        dto.setNombreAdultes(reservation.getNombreAdultes());
        dto.setNombreEnfants(reservation.getNombreEnfants());
        dto.setPrixParNuit(reservation.getPrixParNuit());
        dto.setRemiseJournaliere(reservation.getRemiseJournaliere());
        dto.setRemiseTotale(reservation.getRemiseTotale());
        dto.setMontantTotal(reservation.getMontantTotal());
        dto.setMontantPaye(reservation.getMontantPaye());
        dto.setMontantRestant(reservation.getMontantRestant());
        dto.setStatut(reservation.getStatut());
        dto.setStatutPaiement(reservation.getStatutPaiement());
        dto.setModePaiement(reservation.getModePaiement());
        dto.setNotes(reservation.getNotes());
        dto.setDemandesSpeciales(reservation.getDemandesSpeciales());
        dto.setDateCheckin(reservation.getDateCheckin());
        dto.setDateCheckout(reservation.getDateCheckout());
        dto.setReferenceExterne(reservation.getReferenceExterne());

        if (reservation.getCreatedBy() != null) {
            dto.setCreatedById(reservation.getCreatedBy().getId());
            dto.setCreatedByName(reservation.getCreatedBy().getFirstName() + " " + reservation.getCreatedBy().getLastName());
        }
        if (reservation.getCheckinBy() != null) {
            dto.setCheckinById(reservation.getCheckinBy().getId());
            dto.setCheckinByName(reservation.getCheckinBy().getFirstName() + " " + reservation.getCheckinBy().getLastName());
        }
        if (reservation.getCheckoutBy() != null) {
            dto.setCheckoutById(reservation.getCheckoutBy().getId());
            dto.setCheckoutByName(reservation.getCheckoutBy().getFirstName() + " " + reservation.getCheckoutBy().getLastName());
        }
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());

        return dto;
    }
}