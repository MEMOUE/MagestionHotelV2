package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.ChambreDto;
import com.mghbackend.dto.request.DisponibiliteChambreRequest;
import com.mghbackend.entity.Chambre;
import com.mghbackend.entity.Hotel;
import com.mghbackend.enums.StatutChambre;
import com.mghbackend.enums.TypeChambre;
import com.mghbackend.repository.ChambreRepository;
import com.mghbackend.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChambreService {

    private final ChambreRepository chambreRepository;
    private final HotelRepository hotelRepository;

    public ChambreDto createChambre(Long hotelId, ChambreDto chambreDto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        if (chambreRepository.existsByHotelAndNumero(hotel, chambreDto.getNumero())) {
            throw new BusinessException("Une chambre avec ce numéro existe déjà");
        }

        Chambre chambre = new Chambre();
        chambre.setHotel(hotel);
        chambre.setNumero(chambreDto.getNumero());
        chambre.setType(chambreDto.getType());
        chambre.setPrixParNuit(chambreDto.getPrixParNuit());
        chambre.setCapacite(chambreDto.getCapacite());
        chambre.setSuperficie(chambreDto.getSuperficie());
        chambre.setDescription(chambreDto.getDescription());
        chambre.setStatut(chambreDto.getStatut() != null ? chambreDto.getStatut() : StatutChambre.DISPONIBLE);
        chambre.setEtage(chambreDto.getEtage());
        chambre.setWifi(chambreDto.getWifi() != null ? chambreDto.getWifi() : true);
        chambre.setClimatisation(chambreDto.getClimatisation() != null ? chambreDto.getClimatisation() : true);
        chambre.setTelevision(chambreDto.getTelevision() != null ? chambreDto.getTelevision() : true);
        chambre.setMinibar(chambreDto.getMinibar() != null ? chambreDto.getMinibar() : false);
        chambre.setCoffre(chambreDto.getCoffre() != null ? chambreDto.getCoffre() : false);
        chambre.setBalcon(chambreDto.getBalcon() != null ? chambreDto.getBalcon() : false);
        chambre.setVueMer(chambreDto.getVueMer() != null ? chambreDto.getVueMer() : false);

        if (chambreDto.getImages() != null) {
            chambre.setImages(chambreDto.getImages());
        }

        Chambre savedChambre = chambreRepository.save(chambre);
        return convertToDto(savedChambre);
    }

    @Transactional(readOnly = true)
    public ChambreDto getChambreById(Long id) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Chambre non trouvée"));
        return convertToDto(chambre);
    }

    @Transactional(readOnly = true)
    public List<ChambreDto> getChambresByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return chambreRepository.findByHotel(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreDto> getChambresByHotelAndStatut(Long hotelId, StatutChambre statut) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return chambreRepository.findByHotelAndStatut(hotel, statut).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreDto> getChambresByHotelAndType(Long hotelId, TypeChambre type) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return chambreRepository.findByHotelAndType(hotel, type).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreDto> searchChambres(Long hotelId, String keyword) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return chambreRepository.searchByHotelAndKeyword(hotel, keyword).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChambreDto> getChambresDisponibles(Long hotelId, DisponibiliteChambreRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        if (request.getDateArrivee().isAfter(request.getDateDepart())) {
            throw new BusinessException("La date d'arrivée doit être avant la date de départ");
        }

        List<Chambre> chambres;
        if (request.getTypeChambre() != null) {
            chambres = chambreRepository.findChambresDisponiblesByType(
                    hotel,
                    request.getTypeChambre(),
                    request.getDateArrivee(),
                    request.getDateDepart()
            );
        } else {
            chambres = chambreRepository.findChambresDisponibles(
                    hotel,
                    request.getDateArrivee(),
                    request.getDateDepart()
            );
        }

        // Filtrer par capacité si spécifié
        if (request.getNombrePersonnes() != null) {
            chambres = chambres.stream()
                    .filter(c -> c.getCapacite() >= request.getNombrePersonnes())
                    .collect(Collectors.toList());
        }

        return chambres.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ChambreDto updateChambre(Long id, ChambreDto chambreDto) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Chambre non trouvée"));

        // Vérifier l'unicité du numéro si changé
        if (!chambre.getNumero().equals(chambreDto.getNumero()) &&
                chambreRepository.existsByHotelAndNumero(chambre.getHotel(), chambreDto.getNumero())) {
            throw new BusinessException("Une chambre avec ce numéro existe déjà");
        }

        chambre.setNumero(chambreDto.getNumero());
        chambre.setType(chambreDto.getType());
        chambre.setPrixParNuit(chambreDto.getPrixParNuit());
        chambre.setCapacite(chambreDto.getCapacite());
        chambre.setSuperficie(chambreDto.getSuperficie());
        chambre.setDescription(chambreDto.getDescription());
        chambre.setEtage(chambreDto.getEtage());
        chambre.setStatut(chambreDto.getStatut());
        chambre.setWifi(chambreDto.getWifi());
        chambre.setClimatisation(chambreDto.getClimatisation());
        chambre.setTelevision(chambreDto.getTelevision());
        chambre.setMinibar(chambreDto.getMinibar());
        chambre.setCoffre(chambreDto.getCoffre());
        chambre.setBalcon(chambreDto.getBalcon());
        chambre.setVueMer(chambreDto.getVueMer());

        if (chambreDto.getImages() != null) {
            chambre.setImages(chambreDto.getImages());
        }

        Chambre updatedChambre = chambreRepository.save(chambre);
        return convertToDto(updatedChambre);
    }

    public void updateStatut(Long id, StatutChambre statut) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Chambre non trouvée"));
        chambre.setStatut(statut);
        chambreRepository.save(chambre);
    }

    public void deleteChambre(Long id) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Chambre non trouvée"));

        // Vérifier s'il y a des réservations actives
        if (!chambre.getReservations().isEmpty()) {
            boolean hasActiveReservations = chambre.getReservations().stream()
                    .anyMatch(r -> r.getStatut() == com.mghbackend.enums.StatutReservation.CONFIRMEE ||
                            r.getStatut() == com.mghbackend.enums.StatutReservation.EN_COURS);
            if (hasActiveReservations) {
                throw new BusinessException("Impossible de supprimer une chambre avec des réservations actives");
            }
        }

        chambreRepository.delete(chambre);
    }

    private ChambreDto convertToDto(Chambre chambre) {
        ChambreDto dto = new ChambreDto();
        dto.setId(chambre.getId());
        dto.setNumero(chambre.getNumero());
        dto.setType(chambre.getType());
        dto.setPrixParNuit(chambre.getPrixParNuit());
        dto.setCapacite(chambre.getCapacite());
        dto.setSuperficie(chambre.getSuperficie());
        dto.setDescription(chambre.getDescription());
        dto.setStatut(chambre.getStatut());
        dto.setEtage(chambre.getEtage());
        dto.setWifi(chambre.getWifi());
        dto.setClimatisation(chambre.getClimatisation());
        dto.setTelevision(chambre.getTelevision());
        dto.setMinibar(chambre.getMinibar());
        dto.setCoffre(chambre.getCoffre());
        dto.setBalcon(chambre.getBalcon());
        dto.setVueMer(chambre.getVueMer());
        dto.setHotelId(chambre.getHotel().getId());
        dto.setHotelName(chambre.getHotel().getName());
        dto.setImages(chambre.getImages());
        dto.setCreatedAt(chambre.getCreatedAt());
        dto.setUpdatedAt(chambre.getUpdatedAt());
        return dto;
    }
}