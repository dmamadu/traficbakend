package com.itma.gestionProjet.services.imp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itma.gestionProjet.entities.*;
import com.itma.gestionProjet.repositories.DatabasePapAgricoleRepository;
import com.itma.gestionProjet.repositories.DatabasePapEconomiqueRepository;
import com.itma.gestionProjet.repositories.DatabasePapHabitatRepository;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fiche PAP (écran pap/detail) : lecture d'une PAP par catégorie, dédommagement et photo.
 *
 * Les PAP sont réparties en quatre tables (habitat, agricole, place d'affaires, économique).
 * Chaque opération vérifie que la PAP appartient bien au projet demandé : la permission
 * est contrôlée sur projectId, il ne faut donc pas pouvoir viser la PAP d'un autre projet.
 */
@Service
public class PapFicheService {

    /** Valeur de statutPap lue par le tableau de bord Baseline (BaselineStatsService.statut). */
    public static final String STATUT_INDEMNISATION_TERMINEE = "indemnisation_terminee";

    /** Le projet n'est pas renvoyé dans la fiche (relation lourde, déjà connue du front). */
    @JsonIgnoreProperties({"project", "hibernateLazyInitializer", "handler"})
    private abstract static class SansProjet {}

    private final DatabasePapHabitatRepository habitatRepository;
    private final DatabasePapAgricoleRepository agricoleRepository;
    private final DatabasePapPlaceAffaireRepository placeAffaireRepository;
    private final DatabasePapEconomiqueRepository economiqueRepository;
    private final ObjectMapper mapper;

    public PapFicheService(DatabasePapHabitatRepository habitatRepository,
                           DatabasePapAgricoleRepository agricoleRepository,
                           DatabasePapPlaceAffaireRepository placeAffaireRepository,
                           DatabasePapEconomiqueRepository economiqueRepository,
                           ObjectMapper objectMapper) {
        this.habitatRepository = habitatRepository;
        this.agricoleRepository = agricoleRepository;
        this.placeAffaireRepository = placeAffaireRepository;
        this.economiqueRepository = economiqueRepository;
        this.mapper = objectMapper.copy()
                .addMixIn(DatabasePapHabitat.class, SansProjet.class)
                .addMixIn(DatabasePapAgricole.class, SansProjet.class)
                .addMixIn(DatabasePapPlaceAffaire.class, SansProjet.class)
                .addMixIn(DatabasePapEconomique.class, SansProjet.class);
    }

    /** Catégories acceptées dans l'URL, avec le code `type` attendu par le front. */
    public enum Categorie {
        HABITAT("PAPHABITAT"),
        AGRICOLE("PAPAGRICOLE"),
        PLACE_AFFAIRE("PAPPLACEAFFAIRE"),
        ECONOMIQUE("PAPECONOMIQUE");

        public final String type;

        Categorie(String type) { this.type = type; }

        public static Categorie depuis(String valeur) {
            String v = valeur == null ? "" : valeur.trim().toUpperCase().replace('-', '_');
            for (Categorie c : values()) {
                if (c.name().equals(v) || c.type.equals(v)) return c;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie de PAP inconnue : " + valeur);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lire(Categorie categorie, Long id, Long projectId) {
        Object pap = charger(categorie, id, projectId);
        Map<String, Object> fiche = mapper.convertValue(pap, new TypeReference<Map<String, Object>>() {});
        fiche.put("projectId", projectId);
        if (fiche.get("type") == null || "".equals(fiche.get("type"))) {
            fiche.put("type", categorie.type);
        }
        return fiche;
    }

    @Transactional
    public Map<String, Object> dedommager(Categorie categorie, Long id, Long projectId) {
        modifier(categorie, id, projectId,
                h -> h.setStatutPap(STATUT_INDEMNISATION_TERMINEE),
                a -> a.setStatutPap(STATUT_INDEMNISATION_TERMINEE),
                p -> p.setStatutPap(STATUT_INDEMNISATION_TERMINEE),
                e -> e.setStatutPap(STATUT_INDEMNISATION_TERMINEE));
        return lire(categorie, id, projectId);
    }

    @Transactional
    public Map<String, Object> changerPhoto(Categorie categorie, Long id, Long projectId, String nomFichier) {
        String fichier = nomFichier == null ? "" : nomFichier.trim();
        // Nom de fichier renvoyé par l'upload : pas de chemin, pas d'URL.
        if (fichier.isEmpty() || fichier.length() > 255 || !fichier.matches("[A-Za-z0-9._-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }
        modifier(categorie, id, projectId,
                h -> h.setPhotoPap(fichier),
                a -> a.setPhotoPap(fichier),
                p -> p.setPhotoPap(fichier),
                e -> e.setPhotoPap(fichier));
        return lire(categorie, id, projectId);
    }

    // -------------------------------------------------------------------------

    private Object charger(Categorie categorie, Long id, Long projectId) {
        return switch (categorie) {
            case HABITAT -> verifierProjet(habitatRepository.findById(id).orElse(null),
                    p -> p.getProject(), projectId);
            case AGRICOLE -> verifierProjet(agricoleRepository.findById(id).orElse(null),
                    p -> p.getProject(), projectId);
            case PLACE_AFFAIRE -> verifierProjet(placeAffaireRepository.findById(id).orElse(null),
                    p -> p.getProject(), projectId);
            case ECONOMIQUE -> verifierProjet(economiqueRepository.findById(id).orElse(null),
                    p -> p.getProject(), projectId);
        };
    }

    private void modifier(Categorie categorie, Long id, Long projectId,
                          Consumer<DatabasePapHabitat> habitat,
                          Consumer<DatabasePapAgricole> agricole,
                          Consumer<DatabasePapPlaceAffaire> placeAffaire,
                          Consumer<DatabasePapEconomique> economique) {
        switch (categorie) {
            case HABITAT -> {
                DatabasePapHabitat p = (DatabasePapHabitat) charger(categorie, id, projectId);
                habitat.accept(p);
                habitatRepository.save(p);
            }
            case AGRICOLE -> {
                DatabasePapAgricole p = (DatabasePapAgricole) charger(categorie, id, projectId);
                agricole.accept(p);
                agricoleRepository.save(p);
            }
            case PLACE_AFFAIRE -> {
                DatabasePapPlaceAffaire p = (DatabasePapPlaceAffaire) charger(categorie, id, projectId);
                placeAffaire.accept(p);
                placeAffaireRepository.save(p);
            }
            case ECONOMIQUE -> {
                DatabasePapEconomique p = (DatabasePapEconomique) charger(categorie, id, projectId);
                economique.accept(p);
                economiqueRepository.save(p);
            }
        }
    }

    /** 404 si la PAP n'existe pas ou appartient à un autre projet (on ne révèle pas lequel). */
    private <T> T verifierProjet(T pap, java.util.function.Function<T, Project> projet, Long projectId) {
        Project p = pap == null ? null : projet.apply(pap);
        if (p == null || !Objects.equals(p.getId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PAP introuvable dans ce projet");
        }
        return pap;
    }
}
