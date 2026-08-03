package com.itma.gestionProjet.seeders;

import com.itma.gestionProjet.entities.Permission;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.repositories.PermissionRepository;
import com.itma.gestionProjet.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Assigne un jeu de permissions par défaut aux rôles existants, une seule fois
 * (ne touche jamais un rôle dont les permissions ont déjà été configurées,
 * que ce soit par ce seeder ou par un admin via l'écran de gestion des rôles).
 *
 * Reprend la logique actuelle codée en dur côté frontend (menu.ts / ADMIN_ROLES) :
 * Super Admin et Admin voient tout, les autres rôles n'ont pas accès aux modules
 * d'administration (Projets, Maîtres d'ouvrages, Consultants, Géolocalisation,
 * Utilisateurs, Rôles, Fonctions, Catégories, Journal d'audit).
 */
@Component
@Order(6)
public class RolePermissionSeeder implements CommandLineRunner {

    private static final Set<String> ADMIN_ONLY_MODULES = Set.of(
            "PROJETS", "MAITRES_OUVRAGE", "CONSULTANTS",
            "GEOLOCALISATION_QUARTIERS", "GEOLOCALISATION_POINTS",
            "UTILISATEURS", "ROLES", "FONCTIONS", "CATEGORIES_UTILISATEURS", "AUDIT"
    );

    private static final Set<String> FULL_ACCESS_ROLES = Set.of("Super Admin", "Admin");

    private static final Set<String> RESTRICTED_ROLES = Set.of(
            "Maitre d'ouvrage", "Consultant", "User", "Representant principal"
    );

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Permission> allPermissions = permissionRepository.findAll();
        List<Permission> restrictedPermissions = allPermissions.stream()
                .filter(p -> !ADMIN_ONLY_MODULES.contains(p.getModule()))
                .toList();

        for (String roleName : FULL_ACCESS_ROLES) {
            seedRole(roleName, allPermissions);
        }
        for (String roleName : RESTRICTED_ROLES) {
            seedRole(roleName, restrictedPermissions);
        }
    }

    private void seedRole(String roleName, List<Permission> permissions) {
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            return;
        }
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            System.out.println("Permissions déjà configurées pour le rôle : " + roleName);
            return;
        }
        role.setPermissions(permissions);
        roleRepository.save(role);
        System.out.println("Permissions par défaut assignées au rôle : " + roleName + " (" + permissions.size() + ")");
    }
}
