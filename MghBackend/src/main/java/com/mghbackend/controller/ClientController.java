package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.ClientDto;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_CREER_RESERVATION')")
    public ResponseEntity<ApiResponse<ClientDto>> createClient(
            @Valid @RequestBody ClientDto clientDto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ClientDto client = clientService.createClient(principal.getHotelId(), clientDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Client créé avec succès", client));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ClientDto>> getClient(@PathVariable Long id) {
        try {
            ClientDto client = clientService.getClientById(id);
            return ResponseEntity.ok(ApiResponse.success(client));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ClientDto>>> getClients(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ClientDto> clients = clientService.getClientsByHotel(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(clients));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ClientDto>> getClientByEmail(
            @PathVariable String email,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ClientDto client = clientService.getClientByEmail(principal.getHotelId(), email);
            return ResponseEntity.ok(ApiResponse.success(client));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/telephone/{telephone}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<ClientDto>> getClientByTelephone(
            @PathVariable String telephone,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            ClientDto client = clientService.getClientByTelephone(principal.getHotelId(), telephone);
            return ResponseEntity.ok(ApiResponse.success(client));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ClientDto>>> searchClients(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ClientDto> clients = clientService.searchClients(principal.getHotelId(), keyword);
            return ResponseEntity.ok(ApiResponse.success(clients));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/fideles")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_RESERVATIONS')")
    public ResponseEntity<ApiResponse<List<ClientDto>>> getClientsFideles(
            @RequestParam(defaultValue = "3") int minReservations,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<ClientDto> clients = clientService.getClientsFideles(principal.getHotelId(), minReservations);
            return ResponseEntity.ok(ApiResponse.success(clients));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_RESERVATION')")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientDto clientDto) {
        try {
            ClientDto client = clientService.updateClient(id, clientDto);
            return ResponseEntity.ok(ApiResponse.success("Client mis à jour avec succès", client));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        try {
            clientService.deleteClient(id);
            return ResponseEntity.ok(ApiResponse.success("Client supprimé avec succès", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}