package com.mghbackend.config;

import com.mghbackend.entity.Role;
import com.mghbackend.entity.SystemAdmin;
import com.mghbackend.enums.TypePermission;
import com.mghbackend.repository.RoleRepository;
import com.mghbackend.repository.SystemAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-email:admin@magestionhotel.com}")
    private String defaultAdminEmail;

    @Value("${admin.default-password:Passer@123}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initialisation des données...");

        createOrUpdateRoles();
        createDefaultAdminIfNeeded();

        log.info("Initialisation des données terminée");
    }

    /**
     * Crée le tout premier compte administrateur système au démarrage si aucun n'existe.
     * Email/mot de passe configurables via ADMIN_DEFAULT_EMAIL / ADMIN_DEFAULT_PASSWORD.
     * ⚠️ Le mot de passe par défaut doit être changé immédiatement après la première connexion.
     */
    private void createDefaultAdminIfNeeded() {
        if (systemAdminRepository.count() > 0) {
            return;
        }

        SystemAdmin admin = new SystemAdmin();
        admin.setName("Administrateur");
        admin.setEmail(defaultAdminEmail);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        admin.setActive(true);
        systemAdminRepository.save(admin);

        log.warn("⚠️ Compte administrateur système créé : {} — changez le mot de passe par défaut immédiatement !",
                defaultAdminEmail);
    }

    private void createOrUpdateRoles() {
        // Rôle RECEPTION
        createOrUpdateRole("RECEPTION", "Employé de la réception", new HashSet<>(Set.of(
                TypePermission.VOIR_RESERVATIONS,
                TypePermission.CREER_RESERVATION,
                TypePermission.MODIFIER_RESERVATION,
                TypePermission.ANNULER_RESERVATION,
                TypePermission.VOIR_CONFIGURATION,
                TypePermission.MODIFIER_CONFIGURATION

                )));

        // Rôle RESTAURANT
        createOrUpdateRole("RESTAURANT", "Employé du restaurant", new HashSet<>(Set.of(
                TypePermission.VOIR_COMMANDES,
                TypePermission.CREER_COMMANDE,
                TypePermission.MODIFIER_COMMANDE,
                TypePermission.GERER_MENU,
                TypePermission.VOIR_CONFIGURATION,
                TypePermission.VOIR_RESERVATIONS
        )));

        // Rôle ECONOMAT
        createOrUpdateRole("ECONOMAT", "Employé de l'économat", new HashSet<>(Set.of(
                TypePermission.VOIR_STOCK,
                TypePermission.MODIFIER_STOCK,
                TypePermission.GERER_FOURNISSEURS,
                TypePermission.PASSER_COMMANDES,
                TypePermission.VOIR_CONFIGURATION
        )));

        // Rôle COMPTABLE
        createOrUpdateRole("COMPTABLE", "Employé comptable", new HashSet<>(Set.of(
                TypePermission.VOIR_COMPTABILITE,
                TypePermission.MODIFIER_COMPTABILITE,
                TypePermission.GENERER_RAPPORTS,
                TypePermission.VOIR_CONFIGURATION
        )));

        // Rôle MANAGER
        createOrUpdateRole("MANAGER", "Manager général", new HashSet<>(Set.of(
                TypePermission.VOIR_RESERVATIONS,
                TypePermission.CREER_RESERVATION,
                TypePermission.MODIFIER_RESERVATION,
                TypePermission.ANNULER_RESERVATION,
                TypePermission.VOIR_COMMANDES,
                TypePermission.CREER_COMMANDE,
                TypePermission.MODIFIER_COMMANDE,
                TypePermission.GERER_MENU,
                TypePermission.VOIR_STOCK,
                TypePermission.MODIFIER_STOCK,
                TypePermission.GERER_FOURNISSEURS,
                TypePermission.PASSER_COMMANDES,
                TypePermission.VOIR_COMPTABILITE,
                TypePermission.GENERER_RAPPORTS,
                TypePermission.VOIR_CONFIGURATION,
                TypePermission.MODIFIER_CONFIGURATION,
                TypePermission.GERER_EMPLOYES,
                TypePermission.VOIR_EMPLOYES
        )));
    }

    /**
     * Crée le rôle s'il n'existe pas, ou MET À JOUR ses permissions
     * s'il existe déjà.
     */
    private void createOrUpdateRole(String name, String description,
                                    Set<TypePermission> permissions) {
        var optionalRole = roleRepository.findByName(name);

        if (optionalRole.isPresent()) {
            Role role = optionalRole.get();
            // La comparaison .equals fonctionne car permissions est un Set
            if (!role.getPermissions().equals(permissions)) {
                role.getPermissions().clear();
                role.getPermissions().addAll(permissions);
                role.setDescription(description);
                roleRepository.save(role);
                log.info("Rôle mis à jour: {} — permissions synchronisées", name);
            }
        } else {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setPermissions(permissions);
            roleRepository.save(role);
            log.info("Rôle créé: {}", name);
        }
    }
}