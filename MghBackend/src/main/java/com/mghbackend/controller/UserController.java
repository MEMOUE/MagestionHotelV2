package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.*;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.request.CreateUserRequest;
import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

	private final UserService userService;

	@PostMapping
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_GERER_EMPLOYES')")
	public ResponseEntity<ApiResponse<UserDto>> createUser(
			@Valid @RequestBody CreateUserRequest request,
			@AuthenticationPrincipal CustomUserPrincipal principal) {
		try {
			UserDto user = userService.createUser(principal.getHotelId(), request);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success("Employé créé avec succès", user));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/profile")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<ApiResponse<UserDto>> getUserProfile(
			@AuthenticationPrincipal CustomUserPrincipal principal) {
		try {
			UserDto user = userService.getUserById(principal.getId());
			return ResponseEntity.ok(ApiResponse.success(user));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('HOTEL') or (hasRole('USER') and (#id == authentication.principal.id or hasAuthority('PERMISSION_VOIR_EMPLOYES')))")
	public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
		try {
			UserDto user = userService.getUserById(id);
			return ResponseEntity.ok(ApiResponse.success(user));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_EMPLOYES')")
	public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
			@AuthenticationPrincipal CustomUserPrincipal principal,
			@RequestParam(defaultValue = "false") boolean includeInactive) {
		try {
			List<UserDto> users = includeInactive
					? userService.getUsersByHotel(principal.getHotelId())
					: userService.getActiveUsersByHotel(principal.getHotelId());
			return ResponseEntity.ok(ApiResponse.success(users));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@GetMapping("/search")
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_EMPLOYES')")
	public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(
			@RequestParam String keyword,
			@AuthenticationPrincipal CustomUserPrincipal principal) {
		try {
			List<UserDto> users = userService.searchUsers(principal.getHotelId(), keyword);
			return ResponseEntity.ok(ApiResponse.success(users));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('HOTEL') or (hasRole('USER') and #id == authentication.principal.id) or hasAuthority('PERMISSION_GERER_EMPLOYES')")
	public ResponseEntity<ApiResponse<UserDto>> updateUser(
			@PathVariable Long id,
			@Valid @RequestBody UserDto userDto) {
		try {
			UserDto user = userService.updateUser(id, userDto);
			return ResponseEntity.ok(ApiResponse.success("Employé mis à jour avec succès", user));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/roles")
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_GERER_EMPLOYES')")
	public ResponseEntity<ApiResponse<Void>> updateUserRoles(
			@PathVariable Long id,
			@RequestBody Set<String> roleNames) {
		try {
			userService.updateUserRoles(id, roleNames);
			return ResponseEntity.ok(ApiResponse.success("Rôles mis à jour avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/password")
	@PreAuthorize("hasRole('USER') and #id == authentication.principal.id")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@PathVariable Long id,
			@Valid @RequestBody UpdatePasswordRequest request) {
		try {
			userService.changePassword(id, request);
			return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}

	@PutMapping("/{id}/toggle-status")
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_GERER_EMPLOYES')")
	public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
		try {
			userService.toggleActiveStatus(id);
			return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", null));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}
}
