package com.itma.gestionProjet.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Données du tableau de bord « Mise en œuvre du PAR » (/stats/mise-en-oeuvre).
 *
 * Deux bases de calcul :
 * - PAP : toutes les PAP du projet (3 catégories), filtrées par zone ; sert au budget prévu.
 * - Dossiers : les ententes du projet (agricole + places d'affaires, l'habitat n'a pas encore
 *   de processus d'entente), filtrées par zone ; sert à l'avancement et aux paiements.
 *
 * La période (debut/fin) ne s'applique qu'aux événements datés : paiements (datePaiement) et
 * finalisations (dateFinalisation). Les étapes et les statuts restent une photo à l'instant T.
 */
@Data
public class MiseEnOeuvreStats {

    private LocalDateTime dateMaj;
    private List<String> zones = new ArrayList<>();

    private long totalPap;
    private long totalVulnerables;
    private long totalDossiers;
    private long vulnerablesDossiers;

    private double budgetPrevu;
    private double montantVerse;
    private double resteAPayer;
    private double tauxDecaissement;

    private long dossiersTransmis;
    private long ententesFinalisees;
    private long papPayees;
    private long vulnerablesPayees;

    private List<Etape> etapes = new ArrayList<>();
    private Statuts statuts = new Statuts();
    private List<Categorie> categories = new ArrayList<>();
    private Alertes alertes = new Alertes();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Etape {
        private String code;
        private String libelle;
        private long count;
    }

    @Data
    public static class Statuts {
        private long brouillon;
        private long synchronisee;
        private long finalisee;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Categorie {
        private String code;
        private String libelle;
        private long totalPap;
        private long dossiers;
        private long papPayees;
        private double budget;
        private double verse;
    }

    @Data
    public static class Alertes {
        /** Ententes à l'étape « Accord à obtenir » depuis plus de 30 jours. */
        private long accordsEnRetard;
        /** Dossiers de PAP vulnérables pas encore payés. */
        private long vulnerablesNonPayees;
        /** Ententes synchronisées depuis le terrain, en attente de validation. */
        private long ententesAValider;
    }
}
