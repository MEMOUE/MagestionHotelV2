package com.mghbackend.repository;

import com.mghbackend.entity.Chambre;
import com.mghbackend.entity.Hotel;
import com.mghbackend.enums.StatutChambre;
import com.mghbackend.enums.TypeChambre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChambreRepository extends JpaRepository<Chambre, Long> {

    List<Chambre> findByHotel(Hotel hotel);

    List<Chambre> findByHotelAndStatut(Hotel hotel, StatutChambre statut);

    List<Chambre> findByHotelAndType(Hotel hotel, TypeChambre type);

    Optional<Chambre> findByHotelAndNumero(Hotel hotel, String numero);

    boolean existsByHotelAndNumero(Hotel hotel, String numero);

    @Query("SELECT c FROM Chambre c WHERE c.hotel = :hotel AND " +
            "(c.numero LIKE %:keyword% OR CAST(c.type AS string) LIKE %:keyword%)")
    List<Chambre> searchByHotelAndKeyword(@Param("hotel") Hotel hotel, @Param("keyword") String keyword);

    // Rechercher les chambres disponibles pour une période donnée.
    // Une chambre OCCUPEE/RESERVEE reste réservable dès que ses réservations en cours
    // ne chevauchent pas la période demandée (intervalle semi-ouvert : le jour de départ
    // d'une réservation existante peut être le jour d'arrivée de la nouvelle).
    // Seules EN_MAINTENANCE et HORS_SERVICE bloquent la chambre indépendamment des dates.
    @Query("SELECT c FROM Chambre c WHERE c.hotel = :hotel AND " +
            "c.statut NOT IN ('EN_MAINTENANCE', 'HORS_SERVICE') AND " +
            "c.id NOT IN (SELECT r.chambre.id FROM Reservation r WHERE " +
            "r.statut NOT IN ('ANNULEE', 'TERMINEE') AND " +
            "(r.dateArrivee < :dateDepart AND r.dateDepart > :dateArrivee))")
    List<Chambre> findChambresDisponibles(
            @Param("hotel") Hotel hotel,
            @Param("dateArrivee") LocalDate dateArrivee,
            @Param("dateDepart") LocalDate dateDepart
    );

    // Rechercher les chambres disponibles par type et période (mêmes règles que ci-dessus)
    @Query("SELECT c FROM Chambre c WHERE c.hotel = :hotel AND c.type = :type AND " +
            "c.statut NOT IN ('EN_MAINTENANCE', 'HORS_SERVICE') AND " +
            "c.id NOT IN (SELECT r.chambre.id FROM Reservation r WHERE " +
            "r.statut NOT IN ('ANNULEE', 'TERMINEE') AND " +
            "(r.dateArrivee < :dateDepart AND r.dateDepart > :dateArrivee))")
    List<Chambre> findChambresDisponiblesByType(
            @Param("hotel") Hotel hotel,
            @Param("type") TypeChambre type,
            @Param("dateArrivee") LocalDate dateArrivee,
            @Param("dateDepart") LocalDate dateDepart
    );

    long countByHotel(Hotel hotel);

    long countByHotelAndStatut(Hotel hotel, StatutChambre statut);

    long countByHotelAndType(Hotel hotel, TypeChambre type);
}