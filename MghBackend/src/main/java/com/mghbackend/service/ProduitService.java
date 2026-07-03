package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.MouvementStockDto;
import com.mghbackend.dto.ProduitDto;
import com.mghbackend.entity.*;
import com.mghbackend.enums.TypeMouvement;
import com.mghbackend.enums.TypeProduit;
import com.mghbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final FournisseurRepository fournisseurRepository;
    private final HotelRepository hotelRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final UserRepository userRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    // ─────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────

    public ProduitDto createProduit(Long hotelId, ProduitDto dto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        if (produitRepository.findByCode(dto.getCode()).isPresent()) {
            throw new BusinessException("Un produit avec ce code existe déjà");
        }

        Produit produit = new Produit();
        produit.setHotel(hotel);
        produit.setNom(dto.getNom());
        produit.setCode(dto.getCode());
        produit.setDescription(dto.getDescription());
        produit.setUnite(dto.getUnite());
        produit.setQuantiteStock(dto.getQuantiteStock() != null ? dto.getQuantiteStock() : BigDecimal.ZERO);
        produit.setSeuilAlerte(dto.getSeuilAlerte());
        produit.setPrixUnitaire(dto.getPrixUnitaire());
        produit.setTypeProduit(dto.getTypeProduit() != null ? dto.getTypeProduit() : TypeProduit.AUTRE);
        produit.setDisponible(dto.getDisponible() != null ? dto.getDisponible() : true);
        produit.setImageUrl(dto.getImageUrl());

        if (dto.getFournisseurId() != null) {
            Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                    .orElseThrow(() -> new BusinessException("Fournisseur non trouvé"));
            produit.setFournisseur(fournisseur);
        }

        return convertToDto(produitRepository.save(produit));
    }

    @Transactional(readOnly = true)
    public List<ProduitDto> getProduitsByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return produitRepository.findByHotel(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProduitDto> getProduitsEnRupture(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return produitRepository.findProduitsEnRuptureStock(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Menu restaurant
    // ─────────────────────────────────────────────────────────────

    /**
     * Retourne tous les produits disponibles pour le menu restaurant,
     * filtré par type (BOISSON, ENTREE, PLAT, DESSERT, AUTRE).
     */
    @Transactional(readOnly = true)
    public List<ProduitDto> getMenuParType(Long hotelId, TypeProduit typeProduit) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return produitRepository
                .findByHotelAndTypeProduitAndDisponibleTrue(hotel, typeProduit).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retourne le menu complet (tous types, produits disponibles uniquement).
     */
    @Transactional(readOnly = true)
    public List<ProduitDto> getMenuComplet(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return produitRepository.findByHotelAndDisponibleTrue(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Gestion du stock
    // ─────────────────────────────────────────────────────────────

    public void ajusterStock(Long produitId, BigDecimal quantite,
                             TypeMouvement type, String motif, Long userId) {

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new BusinessException("Produit non trouvé"));

        switch (type) {
            case ENTREE:
                produit.setQuantiteStock(produit.getQuantiteStock().add(quantite));
                // Si le stock revient, on rend le produit disponible
                if (produit.getQuantiteStock().compareTo(BigDecimal.ZERO) > 0) {
                    produit.setDisponible(true);
                }
                break;

            case SORTIE:
                if (produit.getQuantiteStock().compareTo(quantite) < 0) {
                    throw new BusinessException("Stock insuffisant pour le produit : " + produit.getNom());
                }
                produit.setQuantiteStock(produit.getQuantiteStock().subtract(quantite));
                // Si stock épuisé, marquer indisponible automatiquement
                if (produit.getQuantiteStock().compareTo(BigDecimal.ZERO) <= 0) {
                    produit.setDisponible(false);
                }
                break;

            case AJUSTEMENT:
                produit.setQuantiteStock(quantite);
                produit.setDisponible(quantite.compareTo(BigDecimal.ZERO) > 0);
                break;

            case RETOUR:
                produit.setQuantiteStock(produit.getQuantiteStock().add(quantite));
                produit.setDisponible(true);
                break;
        }

        produitRepository.save(produit);

        // Enregistrement du mouvement
        MouvementStock mouvement = new MouvementStock();
        mouvement.setProduit(produit);
        mouvement.setType(type);
        mouvement.setQuantite(quantite);
        mouvement.setMotif(motif);
        mouvement.setHotel(produit.getHotel());

        if (userId != null) {
            userRepository.findById(userId).ifPresent(mouvement::setUser);
        }

        mouvementStockRepository.save(mouvement);
    }

    /**
     * Méthode interne appelée par CommandeRestaurantService pour décrémenter
     * le stock lors de la création / validation d'une commande.
     */
    @Transactional
    public void decrementerStockCommande(Long produitId, BigDecimal quantite, Long hotelId) {
        ajusterStock(produitId, quantite, TypeMouvement.SORTIE,
                "Consommation restaurant", null);
    }

    // ─────────────────────────────────────────────────────────────
    // Conversion
    // ─────────────────────────────────────────────────────────────

    public ProduitDto convertToDto(Produit produit) {
        ProduitDto dto = new ProduitDto();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setCode(produit.getCode());
        dto.setDescription(produit.getDescription());
        dto.setUnite(produit.getUnite());
        dto.setQuantiteStock(produit.getQuantiteStock());
        dto.setSeuilAlerte(produit.getSeuilAlerte());
        dto.setPrixUnitaire(produit.getPrixUnitaire());
        dto.setTypeProduit(produit.getTypeProduit());
        dto.setDisponible(produit.getDisponible());
        dto.setImageUrl(produit.getImageUrl());
        dto.setHotelId(produit.getHotel().getId());

        if (produit.getFournisseur() != null) {
            dto.setFournisseurId(produit.getFournisseur().getId());
            dto.setFournisseurNom(produit.getFournisseur().getNom());
        }

        dto.setCreatedAt(produit.getCreatedAt());
        dto.setUpdatedAt(produit.getUpdatedAt());
        return dto;
    }
    @Transactional(readOnly = true)
    public ProduitDto getProduitById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Produit non trouvé"));
        return convertToDto(produit);
    }

    /**
     * Retourne l'historique complet des mouvements pour un hôtel,
     * trié du plus récent au plus ancien.
     */
    @Transactional(readOnly = true)
    public List<MouvementStockDto> getHistoriqueByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return mouvementStockRepository.findByHotelOrderByDateMouvementDesc(hotel).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retourne l'historique des mouvements pour un produit spécifique.
     */
    @Transactional(readOnly = true)
    public List<MouvementStockDto> getHistoriqueByProduit(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new BusinessException("Produit non trouvé"));
        return mouvementStockRepository.findByProduitOrderByDateMouvementDesc(produit).stream()
                .map(this::convertMouvementToDto)
                .collect(Collectors.toList());
    }

    // Convertisseur MouvementStock → MouvementStockDto
    private MouvementStockDto convertMouvementToDto(MouvementStock m) {
        MouvementStockDto dto = new MouvementStockDto();
        dto.setId(m.getId());
        dto.setProduitId(m.getProduit().getId());
        dto.setProduitNom(m.getProduit().getNom());
        dto.setProduitCode(m.getProduit().getCode());
        dto.setProduitUnite(m.getProduit().getUnite());
        dto.setType(m.getType());
        dto.setQuantite(m.getQuantite());
        dto.setMotif(m.getMotif());
        dto.setDateMouvement(m.getDateMouvement());
        dto.setHotelId(m.getHotel().getId());
        if (m.getUser() != null) {
            dto.setUserId(m.getUser().getId());
            dto.setUserNom(m.getUser().getFirstName() + " " + m.getUser().getLastName());
        }
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }


    public void deleteProduit(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new BusinessException("Produit non trouvé"));

        // 1. Vérifier s'il y a des lignes de commande liées
        List<LigneCommande> lignes = ligneCommandeRepository.findByProduit(produit);
        if (!lignes.isEmpty()) {
            throw new BusinessException(
                    "Impossible de supprimer ce produit : il est utilisé dans "
                            + lignes.size() + " commande(s) restaurant.");
        }

        // 2. Supprimer les mouvements de stock liés
        List<MouvementStock> mouvements = mouvementStockRepository.findByProduit(produit);
        if (!mouvements.isEmpty()) {
            mouvementStockRepository.deleteAll(mouvements);
        }

        // 3. Supprimer le produit
        produitRepository.delete(produit);
    }
}