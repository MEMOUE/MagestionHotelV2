package com.mghbackend.service;

import com.mghbackend.entity.Hotel;
import com.mghbackend.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RapportFinancierScheduler {

    private final HotelRepository hotelRepository;
    private final RapportFinancierService rapportFinancierService;

    /**
     * Envoie le rapport financier complet (toutes les entrées/sorties) du mois écoulé
     * à chaque propriétaire ayant renseigné un email de propriétaire dans les paramètres.
     * Se déclenche juste après minuit le 1er de chaque mois : cela couvre le mois qui
     * vient de se terminer sans dépendre du nombre de jours du mois (28/29/30/31).
     */
    @Scheduled(cron = "0 5 0 1 * *")
    public void envoyerRapportsMensuels() {
        List<Hotel> hotels = hotelRepository.findByActiveTrue();
        log.info("📊 Génération des rapports financiers mensuels pour {} hôtel(s) actif(s)", hotels.size());

        for (Hotel hotel : hotels) {
            try {
                rapportFinancierService.envoyerRapportPourHotel(hotel);
            } catch (Exception e) {
                log.error("❌ Erreur lors de la génération du rapport pour l'hôtel {} : {}",
                        hotel.getName(), e.getMessage());
            }
        }
    }
}
