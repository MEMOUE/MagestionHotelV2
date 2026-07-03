package com.mghbackend.service.impl;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.StatistiquesFinanceDto;
import com.mghbackend.dto.TransactionDto;
import com.mghbackend.entity.CommandeRestaurant;
import com.mghbackend.entity.Reservation;
import com.mghbackend.entity.Transaction;
import com.mghbackend.enums.StatutCommandeRestaurant;
import com.mghbackend.enums.StatutPaiement;
import com.mghbackend.enums.StatutTransaction;
import com.mghbackend.enums.TypeTransaction;
import com.mghbackend.repository.CommandeRestaurantRepository;
import com.mghbackend.repository.ReservationRepository;
import com.mghbackend.repository.TransactionRepository;
import com.mghbackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final CommandeRestaurantRepository commandeRestaurantRepository;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public TransactionDto createTransaction(Long hotelId, TransactionDto dto, Long userId) {
        Transaction transaction = toEntity(dto);
        transaction.setHotelId(hotelId);
        transaction.setStatut(StatutTransaction.EN_ATTENTE);
        transaction.setDateTransaction(
                dto.getDateTransaction() != null ? dto.getDateTransaction() : LocalDateTime.now());
        transaction.setReference(generateReference(hotelId));
        if (userId != null) {
            transaction.setCreatedById(userId);
        }
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        return toDto(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException("Transaction introuvable : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByHotel(Long hotelId) {
        return transactionRepository.findByHotelIdOrderByDateTransactionDesc(hotelId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public TransactionDto updateTransaction(Long id, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction introuvable : " + id));

        if (transaction.getStatut() != StatutTransaction.EN_ATTENTE) {
            throw new BusinessException("Seules les transactions EN_ATTENTE peuvent être modifiées");
        }

        transaction.setType(dto.getType());
        transaction.setCategorie(dto.getCategorie());
        transaction.setMontant(dto.getMontant());
        transaction.setDescription(dto.getDescription());
        transaction.setModePaiement(dto.getModePaiement());
        transaction.setNumeroPiece(dto.getNumeroPiece());
        transaction.setNotes(dto.getNotes());
        transaction.setReservationId(dto.getReservationId());
        transaction.setCommandeRestaurantId(dto.getCommandeRestaurantId());
        if (dto.getDateTransaction() != null) {
            transaction.setDateTransaction(dto.getDateTransaction());
        }
        transaction.setUpdatedAt(LocalDateTime.now());
        return toDto(transactionRepository.save(transaction));
    }

    @Override
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction introuvable : " + id));
        if (transaction.getStatut() != StatutTransaction.EN_ATTENTE) {
            throw new BusinessException("Seules les transactions EN_ATTENTE peuvent être supprimées");
        }
        transactionRepository.deleteById(id);
    }

    // ─── FILTRES ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByHotelAndType(Long hotelId, TypeTransaction type) {
        return transactionRepository
                .findByHotelIdAndTypeOrderByDateTransactionDesc(hotelId, type)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsEnAttente(Long hotelId) {
        return transactionRepository
                .findByHotelIdAndStatutOrderByDateTransactionDesc(hotelId, StatutTransaction.EN_ATTENTE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> searchTransactions(Long hotelId, String keyword) {
        String kw = keyword.toLowerCase();
        return getTransactionsByHotel(hotelId).stream()
                .filter(t -> (t.getReference() != null && t.getReference().toLowerCase().contains(kw))
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(kw))
                        || (t.getCategorie() != null && t.getCategorie().toLowerCase().contains(kw))
                        || (t.getNotes() != null && t.getNotes().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
    }

    // ─── ACTIONS MÉTIER ────────────────────────────────────────────────────────

    @Override
    public TransactionDto validerTransaction(Long id, String validePar) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction introuvable : " + id));
        if (transaction.getStatut() != StatutTransaction.EN_ATTENTE) {
            throw new BusinessException("Seules les transactions EN_ATTENTE peuvent être validées");
        }
        transaction.setStatut(StatutTransaction.VALIDEE);
        transaction.setValidePar(validePar);
        transaction.setDateValidation(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        return toDto(transactionRepository.save(transaction));
    }

    @Override
    public TransactionDto annulerTransaction(Long id, String motif) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction introuvable : " + id));

        if (transaction.getStatut() == StatutTransaction.ANNULEE) {
            throw new BusinessException("La transaction est déjà annulée");
        }

        // On mémorise l'ancien statut AVANT de modifier
        StatutTransaction ancienStatut = transaction.getStatut();

        transaction.setStatut(StatutTransaction.ANNULEE);
        if (motif != null && !motif.isBlank()) {
            String notes = transaction.getNotes() != null
                    ? transaction.getNotes() + "\nMotif d'annulation : " + motif
                    : "Motif d'annulation : " + motif;
            transaction.setNotes(notes);
        }
        transaction.setUpdatedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);

        // Propagation en cascade uniquement si la transaction était VALIDEE et de type REVENU.
        // Une transaction EN_ATTENTE n'a jamais affecté les montants des entités liées.
        if (ancienStatut == StatutTransaction.VALIDEE
                && TypeTransaction.REVENU == transaction.getType()) {
            propaguerAnnulation(transaction);
        }

        return toDto(saved);
    }

    /**
     * Répercute l'annulation d'une transaction VALIDEE sur l'entité liée
     * (réservation ou commande restaurant).
     * Appelée uniquement pour les transactions de type REVENU précédemment VALIDEES.
     */
    private void propaguerAnnulation(Transaction transaction) {
        BigDecimal montant = transaction.getMontant();

        // ── Réservation ──────────────────────────────────────────────────────
        if (transaction.getReservationId() != null) {
            reservationRepository.findById(transaction.getReservationId()).ifPresent(reservation -> {

                // Soustrait le montant annulé — sans passer en négatif
                BigDecimal nouveauMontantPaye = reservation.getMontantPaye()
                        .subtract(montant)
                        .max(BigDecimal.ZERO);

                BigDecimal nouveauMontantRestant = reservation.getMontantTotal()
                        .subtract(nouveauMontantPaye);

                reservation.setMontantPaye(nouveauMontantPaye);
                reservation.setMontantRestant(nouveauMontantRestant);

                // Recalcul du statut de paiement
                if (nouveauMontantPaye.compareTo(BigDecimal.ZERO) == 0) {
                    reservation.setStatutPaiement(StatutPaiement.NON_PAYE);
                } else if (nouveauMontantRestant.compareTo(BigDecimal.ZERO) > 0) {
                    reservation.setStatutPaiement(StatutPaiement.ACOMPTE);
                } else {
                    reservation.setStatutPaiement(StatutPaiement.PAYE);
                }

                reservationRepository.save(reservation);

                log.info("✅ Réservation {} mise à jour après annulation transaction {} : " +
                                "montantPaye={}, montantRestant={}",
                        reservation.getNumeroReservation(),
                        transaction.getReference(),
                        nouveauMontantPaye,
                        nouveauMontantRestant);
            });
        }

        // ── Commande restaurant ──────────────────────────────────────────────
        if (transaction.getCommandeRestaurantId() != null) {
            commandeRestaurantRepository.findById(transaction.getCommandeRestaurantId())
                    .ifPresent(commande -> {

                        BigDecimal nouveauMontantPaye = commande.getMontantPaye()
                                .subtract(montant)
                                .max(BigDecimal.ZERO);

                        commande.setMontantPaye(nouveauMontantPaye);

                        // Si la commande était PAYEE mais que le montant payé
                        // repasse sous le total, on la repasse à SERVIE
                        if (commande.getStatut() == StatutCommandeRestaurant.PAYEE
                                && nouveauMontantPaye.compareTo(commande.getMontantTotal()) < 0) {
                            commande.setStatut(StatutCommandeRestaurant.SERVIE);
                        }

                        commandeRestaurantRepository.save(commande);

                        log.info("✅ Commande {} mise à jour après annulation transaction {} : " +
                                        "montantPaye={}",
                                commande.getNumeroCommande(),
                                transaction.getReference(),
                                nouveauMontantPaye);
                    });
        }
    }

    // ─── STATISTIQUES ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StatistiquesFinanceDto getStatistiques(Long hotelId) {
        List<Transaction> all = transactionRepository.findByHotelIdOrderByDateTransactionDesc(hotelId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debutJour = now.toLocalDate().atStartOfDay();
        LocalDateTime debutMois = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        BigDecimal totalRevenus  = sumByType(all, TypeTransaction.REVENU,  StatutTransaction.VALIDEE);
        BigDecimal totalDepenses = sumByType(all, TypeTransaction.DEPENSE, StatutTransaction.VALIDEE);
        BigDecimal revenusMois   = sumByTypeAndPeriod(all, TypeTransaction.REVENU,  StatutTransaction.VALIDEE, debutMois, now);
        BigDecimal depensesMois  = sumByTypeAndPeriod(all, TypeTransaction.DEPENSE, StatutTransaction.VALIDEE, debutMois, now);
        BigDecimal revenusJour   = sumByTypeAndPeriod(all, TypeTransaction.REVENU,  StatutTransaction.VALIDEE, debutJour, now);
        BigDecimal depensesJour  = sumByTypeAndPeriod(all, TypeTransaction.DEPENSE, StatutTransaction.VALIDEE, debutJour, now);

        List<Transaction> enAttente = all.stream()
                .filter(t -> t.getStatut() == StatutTransaction.EN_ATTENTE)
                .collect(Collectors.toList());

        BigDecimal montantEnAttente = enAttente.stream()
                .map(Transaction::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Top catégories (transactions validées du mois)
        Map<String, BigDecimal> catMap = all.stream()
                .filter(t -> t.getStatut() == StatutTransaction.VALIDEE
                        && t.getDateTransaction() != null
                        && !t.getDateTransaction().isBefore(debutMois))
                .collect(Collectors.groupingBy(
                        t -> t.getCategorie() != null ? t.getCategorie() : "Autre",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getMontant, BigDecimal::add)));

        List<StatistiquesFinanceDto.TopCategorie> topCategories = catMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    StatistiquesFinanceDto.TopCategorie tc = new StatistiquesFinanceDto.TopCategorie();
                    tc.setCategorie(e.getKey());
                    tc.setMontant(e.getValue());
                    return tc;
                }).collect(Collectors.toList());

        // Évolution des 6 derniers mois
        List<StatistiquesFinanceDto.EvolutionMensuelle> evolution = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        for (int i = 5; i >= 0; i--) {
            YearMonth ym    = YearMonth.now().minusMonths(i);
            LocalDateTime debut = ym.atDay(1).atStartOfDay();
            LocalDateTime fin   = ym.atEndOfMonth().atTime(23, 59, 59);
            BigDecimal rev = sumByTypeAndPeriod(all, TypeTransaction.REVENU,  StatutTransaction.VALIDEE, debut, fin);
            BigDecimal dep = sumByTypeAndPeriod(all, TypeTransaction.DEPENSE, StatutTransaction.VALIDEE, debut, fin);
            StatistiquesFinanceDto.EvolutionMensuelle em = new StatistiquesFinanceDto.EvolutionMensuelle();
            em.setMois(ym.format(fmt));
            em.setRevenus(rev);
            em.setDepenses(dep);
            em.setResultat(rev.subtract(dep));
            evolution.add(em);
        }

        StatistiquesFinanceDto stats = new StatistiquesFinanceDto();
        stats.setTotalRevenus(totalRevenus);
        stats.setTotalDepenses(totalDepenses);
        stats.setSolde(totalRevenus.subtract(totalDepenses));
        stats.setSoldeActuel(totalRevenus.subtract(totalDepenses));
        stats.setRevenusMois(revenusMois);
        stats.setDepensesMois(depensesMois);
        stats.setRevenusJour(revenusJour);
        stats.setDepensesJour(depensesJour);
        stats.setResultatMois(revenusMois.subtract(depensesMois));
        stats.setNombreTransactions(all.size());
        stats.setTransactionsEnAttente(enAttente.size());
        stats.setMontantEnAttente(montantEnAttente);
        stats.setTopCategories(topCategories);
        stats.setEvolutionMensuelle(evolution);
        return stats;
    }

    // ─── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportTransactions(Long hotelId, String format, String dateDebut, String dateFin) {
        List<TransactionDto> transactions = getTransactionsByHotel(hotelId);
        StringBuilder sb = new StringBuilder();
        sb.append("Référence;Type;Catégorie;Montant;Date;Statut;Description\n");
        transactions.forEach(t -> sb.append(String.format("%s;%s;%s;%s;%s;%s;%s\n",
                nvl(t.getReference()), nvl(t.getType()), nvl(t.getCategorie()),
                nvl(t.getMontant()), nvl(t.getDateTransaction()),
                nvl(t.getStatut()), nvl(t.getDescription()))));
        return sb.toString().getBytes();
    }

    // ─── HELPERS PRIVÉS ────────────────────────────────────────────────────────

    private String generateReference(Long hotelId) {
        long count = transactionRepository.countByHotelId(hotelId) + 1;
        return String.format("TRX-%d-%05d", java.time.Year.now().getValue(), count);
    }

    private BigDecimal sumByType(List<Transaction> list, TypeTransaction type, StatutTransaction statut) {
        return list.stream()
                .filter(t -> type == t.getType() && statut == t.getStatut())
                .map(Transaction::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByTypeAndPeriod(List<Transaction> list, TypeTransaction type,
                                          StatutTransaction statut,
                                          LocalDateTime debut, LocalDateTime fin) {
        return list.stream()
                .filter(t -> type == t.getType() && statut == t.getStatut()
                        && t.getDateTransaction() != null
                        && !t.getDateTransaction().isBefore(debut)
                        && !t.getDateTransaction().isAfter(fin))
                .map(Transaction::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nvl(Object o) {
        return o != null ? o.toString() : "";
    }

    private TransactionDto toDto(Transaction t) {
        TransactionDto dto = new TransactionDto();
        dto.setId(t.getId());
        dto.setReference(t.getReference());
        dto.setType(t.getType());
        dto.setCategorie(t.getCategorie());
        dto.setMontant(t.getMontant());
        dto.setDateTransaction(t.getDateTransaction());
        dto.setDescription(t.getDescription());
        dto.setModePaiement(t.getModePaiement());
        dto.setStatut(t.getStatut());
        dto.setReservationId(t.getReservationId());
        dto.setCommandeRestaurantId(t.getCommandeRestaurantId());
        dto.setNumeroPiece(t.getNumeroPiece());
        dto.setNotes(t.getNotes());
        dto.setValidePar(t.getValidePar());
        dto.setDateValidation(t.getDateValidation());
        dto.setHotelId(t.getHotelId());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }

    private Transaction toEntity(TransactionDto dto) {
        Transaction t = new Transaction();
        t.setType(dto.getType());
        t.setCategorie(dto.getCategorie());
        t.setMontant(dto.getMontant());
        t.setDateTransaction(dto.getDateTransaction());
        t.setDescription(dto.getDescription());
        t.setModePaiement(dto.getModePaiement());
        t.setReservationId(dto.getReservationId());
        t.setCommandeRestaurantId(dto.getCommandeRestaurantId());
        t.setNumeroPiece(dto.getNumeroPiece());
        t.setNotes(dto.getNotes());
        return t;
    }
}