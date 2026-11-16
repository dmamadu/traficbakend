package com.itma.gestionProjet.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Données du tableau de bord « BaseLine PAR » (/stats/baseline), filtrées par zone (commune)
 * et éventuellement par catégorie. Les comptages par sexe et vulnérabilité viennent de
 * StatsHelper.buildCategoryStats, la même logique que /stats/combine.
 */
@Data
public class BaselineStats {

    private LocalDateTime dateMaj;
    private String statutProjet;
    private List<String> zones = new ArrayList<>();
    /** Nombre de zones (communes) distinctes parmi les PAP retenues. */
    private long nbZones;

    private CategoryStats placeAffaire;
    private CategoryStats agricole;
    private CategoryStats habitat;
    private CategoryStats total;

    private Avancement avancement = new Avancement();
    private List<PointPap> points = new ArrayList<>();

    /**
     * Statut d'indemnisation de chaque PAP :
     * - terminee : entente payée (étapes Formation, Suivi ou Processus terminé) ou statutPap « indemnisation_terminee » ;
     * - enEtude  : entente entre « PAP à informer » et « Paiement à effectuer », ou statutPap « en_etude » ;
     * - recense  : statutPap « recense » (valeur par défaut à l'import) ;
     * - inconnu  : aucun des cas ci-dessus (statutPap vide ou autre valeur).
     */
    @Data
    public static class Avancement {
        private long recense;
        private long enEtude;
        private long terminee;
        private long inconnu;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointPap {
        private String codePap;
        private String prenom;
        private String nom;
        private String categorie;
        /** recense | en_etude | terminee | inconnu */
        private String statut;
        private String zone;
        private double lat;
        private double lng;
    }
}
