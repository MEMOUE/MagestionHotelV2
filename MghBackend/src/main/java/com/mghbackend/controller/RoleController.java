package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.entity.Role;
import com.mghbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoleController {

	private final RoleRepository roleRepository;

	@GetMapping
	@PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_GERER_EMPLOYES')")
	public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
		try {
			List<Role> roles = roleRepository.findAll();
			return ResponseEntity.ok(ApiResponse.success(roles));
		} catch (BusinessException e) {
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		}
	}
}