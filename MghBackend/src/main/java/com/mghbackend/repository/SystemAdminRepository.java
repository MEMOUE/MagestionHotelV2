package com.mghbackend.repository;

import com.mghbackend.entity.SystemAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemAdminRepository extends JpaRepository<SystemAdmin, Long> {

    Optional<SystemAdmin> findByEmail(String email);

    Optional<SystemAdmin> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);
}
