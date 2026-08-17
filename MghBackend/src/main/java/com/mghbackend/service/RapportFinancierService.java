package com.mghbackend.service;

import com.mghbackend.entity.Hotel;
import com.mghbackend.entity.Transaction;
import com.mghbackend.enums.StatutTransaction;
import com.mghbackend.enums.TypeTransaction;
import com.mghbackend.exception.BusinessException;
import com.mghbackend.repository.HotelRepository;
import com.mghbackend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Calcule et envoie le rapport financier complet (entrées/sorties) d'un hôtel à son
 * propriétaire, que ce soit automatiquement en fin de mois ({@link RapportFinancierScheduler})
 * ou à la demande d'un administrateur système.
 */
@Service
@RequiredArgsConstructor
public class RapportFinancierService {

    private final HotelRepository hotelRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    /** Mois couvert par défaut : le mois calendaire complet précédent. */
    public YearMonth moisParDefaut() {
        return YearMonth.now().minusMonths(1);
    }

    /**
     * Envoi automatique (tâche planifiée) : n'envoie que si un email propriétaire est
     * configuré, sinon ignore silencieusement l'hôtel — pas d'erreur pour ce cas attendu.
     */
    public void envoyerRapportPourHotel(Hotel hotel) {
        String ownerEmail = hotel.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return;
        }
        YearMonth mois = moisParDefaut();
        RapportData data = calculerRapport(hotel, mois);
        emailService.sendMonthlyReportEmail(
                ownerEmail, hotel.getName(), mois, data.transactions(), data.totalRevenus(), data.totalDepenses());
    }

    /**
     * Envoi à la demande par l'administrateur système : propage une erreur explicite
     * si aucun email propriétaire n'est configuré, ou si l'envoi échoue.
     */
    public void envoyerRapportMaintenant(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        String ownerEmail = hotel.getOwnerEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            throw new BusinessException(
                    "Aucun email de propriétaire n'est configuré pour cet hôtel. " +
                    "Demandez à l'hôtel de le renseigner dans ses paramètres avant d'envoyer un rapport.");
        }

        YearMonth mois = moisParDefaut();
        RapportData data = calculerRapport(hotel, mois);
        emailService.sendMonthlyReportEmailNow(
                ownerEmail, hotel.getName(), mois, data.transactions(), data.totalRevenus(), data.totalDepenses());
    }

    private RapportData calculerRapport(Hotel hotel, YearMonth mois) {
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository
                .findByHotelIdAndDateTransactionBetweenOrderByDateTransactionAsc(hotel.getId(), debut, fin);

        BigDecimal totalRevenus = sumByType(transactions, TypeTransaction.REVENU);
        BigDecimal totalDepenses = sumByType(transactions, TypeTransaction.DEPENSE);
        return new RapportData(transactions, totalRevenus, totalDepenses);
    }

    private BigDecimal sumByType(List<Transaction> transactions, TypeTransaction type) {
        return transactions.stream()
                .filter(t -> t.getType() == type && t.getStatut() == StatutTransaction.VALIDEE)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record RapportData(List<Transaction> transactions, BigDecimal totalRevenus, BigDecimal totalDepenses) {}
}
