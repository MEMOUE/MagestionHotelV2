package com.mghbackend.service;

import com.mghbackend.dto.request.UpdatePasswordRequest;
import com.mghbackend.entity.SystemAdmin;
import com.mghbackend.exception.BusinessException;
import com.mghbackend.repository.SystemAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemAdminService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(Long id, UpdatePasswordRequest request) {
        SystemAdmin admin = systemAdminRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Administrateur non trouvé"));

        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw new BusinessException("Ancien mot de passe incorrect");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        systemAdminRepository.save(admin);
    }
}
