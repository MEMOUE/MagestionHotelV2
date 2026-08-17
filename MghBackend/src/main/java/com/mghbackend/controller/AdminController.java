package com.mghbackend.controller;

import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.exception.BusinessException;
import com.mghbackend.service.SystemAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final SystemAdminService systemAdminService;

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN') and #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePasswordRequest request) {
        try {
            systemAdminService.changePassword(id, request);
            return ResponseEntity.ok(ApiResponse.success("Mot de passe modifié avec succès", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
