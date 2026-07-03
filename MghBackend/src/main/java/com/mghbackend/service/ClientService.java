package com.mghbackend.service;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.ClientDto;
import com.mghbackend.entity.Client;
import com.mghbackend.entity.Hotel;
import com.mghbackend.repository.ClientRepository;
import com.mghbackend.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final HotelRepository hotelRepository;

    public ClientDto createClient(Long hotelId, ClientDto clientDto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));

        // Vérifier si un client avec cet email existe déjà
        if (clientDto.getEmail() != null && !clientDto.getEmail().isEmpty() &&
                clientRepository.existsByHotelAndEmail(hotel, clientDto.getEmail())) {
            throw new BusinessException("Un client avec cet email existe déjà");
        }

        Client client = new Client();
        client.setHotel(hotel);
        client.setPrenom(clientDto.getPrenom());
        client.setNom(clientDto.getNom());
        client.setEmail(clientDto.getEmail());
        client.setTelephone(clientDto.getTelephone());
        client.setPieceIdentite(clientDto.getPieceIdentite());
        client.setTypePiece(clientDto.getTypePiece());
        client.setDateNaissance(clientDto.getDateNaissance());
        client.setNationalite(clientDto.getNationalite());
        client.setAdresse(clientDto.getAdresse());
        client.setVille(clientDto.getVille());
        client.setPays(clientDto.getPays());
        client.setNotes(clientDto.getNotes());

        Client savedClient = clientRepository.save(client);
        return convertToDto(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientDto getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));
        return convertToDto(client);
    }

    @Transactional(readOnly = true)
    public ClientDto getClientByEmail(Long hotelId, String email) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        Client client = clientRepository.findByHotelAndEmail(hotel, email)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));
        return convertToDto(client);
    }

    @Transactional(readOnly = true)
    public ClientDto getClientByTelephone(Long hotelId, String telephone) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        Client client = clientRepository.findByHotelAndTelephone(hotel, telephone)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));
        return convertToDto(client);
    }

    @Transactional(readOnly = true)
    public List<ClientDto> getClientsByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return clientRepository.findByHotel(hotel).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClientDto> searchClients(Long hotelId, String keyword) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return clientRepository.searchByHotelAndKeyword(hotel, keyword).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClientDto> getClientsFideles(Long hotelId, int minReservations) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Hôtel non trouvé"));
        return clientRepository.findClientsFideles(hotel, minReservations).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ClientDto updateClient(Long id, ClientDto clientDto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));

        // Vérifier l'unicité de l'email si changé
        if (clientDto.getEmail() != null && !clientDto.getEmail().isEmpty() &&
                !client.getEmail().equals(clientDto.getEmail()) &&
                clientRepository.existsByHotelAndEmail(client.getHotel(), clientDto.getEmail())) {
            throw new BusinessException("Un client avec cet email existe déjà");
        }

        client.setPrenom(clientDto.getPrenom());
        client.setNom(clientDto.getNom());
        client.setEmail(clientDto.getEmail());
        client.setTelephone(clientDto.getTelephone());
        client.setPieceIdentite(clientDto.getPieceIdentite());
        client.setTypePiece(clientDto.getTypePiece());
        client.setDateNaissance(clientDto.getDateNaissance());
        client.setNationalite(clientDto.getNationalite());
        client.setAdresse(clientDto.getAdresse());
        client.setVille(clientDto.getVille());
        client.setPays(clientDto.getPays());
        client.setNotes(clientDto.getNotes());

        Client updatedClient = clientRepository.save(client);
        return convertToDto(updatedClient);
    }

    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client non trouvé"));

        // Vérifier s'il y a des réservations
        if (!client.getReservations().isEmpty()) {
            throw new BusinessException("Impossible de supprimer un client avec des réservations");
        }

        clientRepository.delete(client);
    }

    private ClientDto convertToDto(Client client) {
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setPrenom(client.getPrenom());
        dto.setNom(client.getNom());
        dto.setEmail(client.getEmail());
        dto.setTelephone(client.getTelephone());
        dto.setPieceIdentite(client.getPieceIdentite());
        dto.setTypePiece(client.getTypePiece());
        dto.setDateNaissance(client.getDateNaissance());
        dto.setNationalite(client.getNationalite());
        dto.setAdresse(client.getAdresse());
        dto.setVille(client.getVille());
        dto.setPays(client.getPays());
        dto.setNotes(client.getNotes());
        dto.setHotelId(client.getHotel().getId());
        dto.setHotelName(client.getHotel().getName());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        return dto;
    }
}