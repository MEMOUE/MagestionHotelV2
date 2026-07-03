package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.*;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.request.CreateHotelRequest;
import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HotelController {

	private final HotelService hotelService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<HotelDto>> createHotel(@Valid @RequestBody CreateHotelRequest request) {
		try {
			HotelDto hotel = hotelService.createHotel(request);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success("Hôtel créé avec succès", hotel));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/profile")
	@PreAuthorize("hasRole('HOTEL')")
	public ResponseEntity<ApiResponse<HotelDto>> getProfile(@AuthenticationPrincipal CustomUserPrincipal principal) {
		try {
			HotelDto hotel = hotelService.getHotelById(principal.getId());
			return ResponseEntity.ok(ApiResponse.success(hotel));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('HOTEL') or (hasRole('USER') and #id == authentication.principal.hotelId)")
	public ResponseEntity<ApiResponse<HotelDto>> getHotel(@PathVariable Long id) {
		try {
			HotelDto hotel = hotelService.getHotelById(id);
			return ResponseEntity.ok(ApiResponse.success(hotel));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('HOTEL') and #id == authentication.principal.id")
	public ResponseEntity<ApiResponse<HotelDto>> updateHotel(
			@PathVariable Long id,
			@Valid @RequestBody HotelDto hotelDto) {
		try {
			HotelDto hotel = hotelService.updateHotel(id, hotelDto);
			return ResponseEntity.ok(ApiResponse.success("Hôtel mis à jour avec succès", hotel));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/password")
	@PreAuthorize("hasRole('HOTEL') and #id == authentication.principal.id")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@PathVariable Long id,
			@Valid @RequestBody UpdatePasswordRequest request) {
		try {
			hotelService.changePassword(id, request);
			return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/toggle-status")
	@PreAuthorize("hasRole('ADMIN')") // Pour un futur rôle administrateur système
	public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
		try {
			hotelService.toggleActiveStatus(id);
			return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/extend-subscription")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> extendSubscription(
			@PathVariable Long id,
			@RequestParam int months) {
		try {
			hotelService.extendSubscription(id, months);
			return ResponseEntity.ok(ApiResponse.success("Abonnement prolongé avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<HotelDto>>> getAllHotels() {
		try {
			List<HotelDto> hotels = hotelService.getAllHotels();
			return ResponseEntity.ok(ApiResponse.success(hotels));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<HotelDto>>> searchHotels(@RequestParam String keyword) {
		try {
			List<HotelDto> hotels = hotelService.searchHotels(keyword);
			return ResponseEntity.ok(ApiResponse.success(hotels));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}
}
