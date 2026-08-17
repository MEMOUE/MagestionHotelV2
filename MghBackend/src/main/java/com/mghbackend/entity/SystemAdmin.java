package com.mghbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Compte administrateur de la plateforme (distinct des comptes Hotel et Employé).
 * Peut gérer l'ensemble des hôtels : activer/désactiver, supprimer, prolonger l'abonnement.
 */
@Entity
@Table(name = "system_admins")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemAdmin extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean active = true;
}
