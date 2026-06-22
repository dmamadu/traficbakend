package com.itma.gestionProjet.seeders;

import com.itma.gestionProjet.entities.Categorie;
import com.itma.gestionProjet.entities.Fonction;
import com.itma.gestionProjet.repositories.CategorieRepository;
import com.itma.gestionProjet.repositories.FonctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(4)
public class FonctionSeeder implements CommandLineRunner {

    @Autowired
    private FonctionRepository fonctionRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Override
    public void run(String... args) throws Exception {

        Categorie niveau1 = categorieRepository.findByLibelle("Niveau 1");
        Categorie niveau2 = categorieRepository.findByLibelle("Niveau 2");
        Categorie niveau3 = categorieRepository.findByLibelle("Niveau 3");

        if (niveau1 == null || niveau2 == null || niveau3 == null) {
            System.out.println("[FonctionSeeder] Catégories manquantes — seeder ignoré.");
            return;
        }

        // libelle → categorie
        Map<String, Categorie> fonctions = new LinkedHashMap<>();

        // Niveau 1 — Direction / Pilotage
        fonctions.put("Directeur de projet",             niveau1);
        fonctions.put("Chef de mission",                 niveau1);
        fonctions.put("Responsable PAR",                 niveau1);
        fonctions.put("Coordinateur de réinstallation",  niveau1);

        // Niveau 2 — Expertise / Encadrement
        fonctions.put("Responsable juridique",           niveau2);
        fonctions.put("Responsable financier",           niveau2);
        fonctions.put("Chargé de suivi-évaluation",      niveau2);
        fonctions.put("Expert sociologue",               niveau2);
        fonctions.put("Expert foncier",                  niveau2);
        fonctions.put("Expert en genre et vulnérabilités", niveau2);
        fonctions.put("Expert en compensation",          niveau2);
        fonctions.put("Topographe / Géomètre",           niveau2);

        // Niveau 3 — Terrain / Opérationnel
        fonctions.put("Agent de terrain",                niveau3);
        fonctions.put("Agent de recensement",            niveau3);
        fonctions.put("Représentant des PAP",            niveau3);
        fonctions.put("Membre comité de plaintes",       niveau3);

        for (Map.Entry<String, Categorie> entry : fonctions.entrySet()) {
            String libelle = entry.getKey();
            Categorie categorie = entry.getValue();

            if (!fonctionRepository.existsByLibelle(libelle)) {
                Fonction fonction = new Fonction();
                fonction.setLibelle(libelle);
                fonction.setCategorieRattache(categorie);
                fonctionRepository.save(fonction);
                System.out.println("[FonctionSeeder] Fonction ajoutée : " + libelle + " (" + categorie.getLibelle() + ")");
            } else {
                System.out.println("[FonctionSeeder] Fonction déjà existante : " + libelle);
            }
        }
    }
}