package com.itma.gestionProjet.seeders;

import com.itma.gestionProjet.entities.Permission;
import com.itma.gestionProjet.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Génère le catalogue des permissions (module x action) décrit dans GESTION_PERMISSIONS_DOC.md §4.
 * Un module = un écran réel de la sidebar. Le code de permission = MODULE_ACTION.
 */
@Component
@Order(5)
public class PermissionSeeder implements CommandLineRunner {

    private record ModuleDef(String code, String libelle, String... actions) {
    }

    private static final Map<String, String> ACTION_LIBELLES = Map.ofEntries(
            Map.entry("VOIR", "Voir"),
            Map.entry("CREER", "Créer"),
            Map.entry("MODIFIER", "Modifier"),
            Map.entry("SUPPRIMER", "Supprimer"),
            Map.entry("EXPORTER", "Exporter"),
            Map.entry("IMPORTER", "Importer"),
            Map.entry("VALIDER", "Valider"),
            Map.entry("ACTIVER_DESACTIVER", "Activer / désactiver"),
            Map.entry("GERER_PERMISSIONS", "Gérer les permissions")
    );

    private static final String V = "VOIR", C = "CREER", M = "MODIFIER", S = "SUPPRIMER", E = "EXPORTER";

    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("DASHBOARD_BASELINE", "Dashboard Baseline PAR", V, E),
            new ModuleDef("DASHBOARD_MISE_EN_OEUVRE", "Dashboard Mise en Œuvre", V, E),
            new ModuleDef("PROJETS", "Projets", V, C, M, S),
            new ModuleDef("MAITRES_OUVRAGE", "Maîtres d'ouvrages", V, C, M, S),
            new ModuleDef("PAP", "Parties Affectées (PAP)", V, C, M, S, E, "IMPORTER"),
            new ModuleDef("PIP", "Parties Intéressées (PIP)", V, C, M, S, E),
            new ModuleDef("CONSULTANTS", "Consultants", V, C, M, S),
            new ModuleDef("TACHES", "Tâches", V, C, M, S),
            new ModuleDef("ENTENTE_COMPENSATION", "Entente de compensation", V, C, M, S, "VALIDER"),
            new ModuleDef("PLAINTES", "Mécanisme de gestion des plaintes", V, C, M, S, E),
            new ModuleDef("ACCOMPAGNEMENT_SOCIAL", "Accompagnement social", V, C, M, S),
            new ModuleDef("RESTAURATION_SUBSISTANCE", "Restauration des moyens de subsistance", V, C, M, S),
            new ModuleDef("NEGOCIATION_CONCILIATION", "Négociation et conciliation", V, C, M, S),
            new ModuleDef("PAIEMENT_COMPENSATIONS", "Paiement des compensations", V, C, M, S, "VALIDER"),
            new ModuleDef("ENGAGEMENTS_PARTIES", "Engagements des parties prenantes", V, C, M, S),
            new ModuleDef("DOCUMENTS", "Documents", V, C, M, S, E),
            new ModuleDef("CATEGORIES_DOCUMENTS", "Catégories de documents", V, C, M, S),
            new ModuleDef("ELABORATION_PAR", "Élaboration du PAR", V, C, M, S, E),
            new ModuleDef("GEOLOCALISATION_QUARTIERS", "Géolocalisation — Quartiers", V, C, M, S),
            new ModuleDef("GEOLOCALISATION_POINTS", "Géolocalisation — Points de repère", V, C, M, S),
            new ModuleDef("UTILISATEURS", "Utilisateurs", V, C, M, S, "ACTIVER_DESACTIVER"),
            new ModuleDef("ROLES", "Rôles", V, C, M, S, "GERER_PERMISSIONS"),
            new ModuleDef("FONCTIONS", "Fonctions", V, C, M, S),
            new ModuleDef("CATEGORIES_UTILISATEURS", "Catégories utilisateurs", V, C, M, S),
            new ModuleDef("AUDIT", "Journal d'audit", V, E)
    );

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        for (ModuleDef module : MODULES) {
            for (String action : module.actions()) {
                String code = module.code() + "_" + action;
                if (permissionRepository.existsByCode(code)) {
                    continue;
                }
                Permission permission = new Permission();
                permission.setCode(code);
                permission.setModule(module.code());
                permission.setLibelle(ACTION_LIBELLES.get(action) + " — " + module.libelle());
                permissionRepository.save(permission);
                System.out.println("Permission ajoutée : " + code);
            }
        }
    }
}
