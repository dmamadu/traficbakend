package com.itma.gestionProjet.dtos;


import com.itma.gestionProjet.entities.EtatProcessusEntente;
import com.itma.gestionProjet.entities.StatutEntente;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.Data;

import java.time.LocalDateTime;

//@Data
//public class EntenteDetailsDTO {
//    private Long ententeId;
//    private StatutEntente statut;
//    private EtatProcessusEntente etatProcessus;
//    private String modePaiement;
//    private LocalDateTime dateCreation;
//    private LocalDateTime dateSynchronisation;
//    private LocalDateTime dateFinalisation;
//
//    // Informations du PAP
//    private Long papId;
//    private String papType;
//    private String codePap;
//    private String prenom;
//    private String nom;
//    private String sexe;
//    private String categorie;
//    private String commune;
//    private String departement;
//
//    // Compensation
//    private Double perteTotale;
//    private Double fraisDeplacement;
//    private Double appuiRelocalisation;
//
//    // État des 6 étapes
//    private Boolean compensationEtablie;
//    private Boolean papInformee;
//    private Boolean accordPapObtenu;
//    private Boolean paiementEffectue;
//    private Boolean formationDonnee;
//    private Boolean suiviEffectue;
//}
@Data
public class EntenteDetailsDTO {
    private Long ententeId;
    private StatutEntente statut;
    private EtatProcessusEntente etatProcessus;


    // Nouveaux champs spécifiques aux PAP Agricoles
    private Double perteTerre;
    private String superficie;
    private String codeParcelle;
    private String caracteristiqueParcelle;
    private String activitePrincipale;
    private Long age;


    // Informations PAP de base
    private Long papId;
    private String papType;
    private String codePap;
    private String prenom;
    private String nom;
    private String sexe;
    private String categorie;
    private String commune;
    private String departement;



    private String typeFormation;
    private String formateur;

    private String resultatSuivi;
    private String commentairesSuivi;

//    private String modePaiement;

    private String modeInformation;
    private String detailsInformation;



    // Contact
    private String numeroTelephone;

    // Compensation et pertes
    private Double perteTotale;
    private Double fraisDeplacement;
    private Double appuiRelocalisation;
    private Double perteRevenue;
    private Double perteBatiment;
    private Double perteLoyer;
    private Double perteCloture;
    private Double perteTotaleArbre;
    private Double perteArbreJeune;
    private Double perteArbreAdulte;
    private Double perteEquipement;

    // Paiement
    private String optionPaiement;

    // Géolocalisation
    private String pointGeometriques;

    // Description
    private String description;
    private String evaluationPerte;

    // Statut
    private String statutPap;
    private String vulnerabilite;

    // État du processus d'entente (les 6 étapes)
    private Boolean compensationEtablie;
    private Boolean papInformee;
    private Boolean accordPapObtenu;
    private Boolean paiementEffectue;
    private Boolean formationDonnee;
    private Boolean suiviEffectue;

    // Dates importantes
    private LocalDateTime dateCreation;
    private LocalDateTime dateSynchronisation;
    private LocalDateTime dateFinalisation;

    private LocalDateTime datePaiement;
    private LocalDateTime dateFormation;
    private LocalDateTime dateSuivi;
    private LocalDateTime dateAccordPap;
    private LocalDateTime dateInformationPap;

    // Pièces du processus : signature de la PAP (accord) et preuve de paiement (URL de fichier)
    private String preuveAccord;
    private String referencePaiement;
    private LocalDateTime dateEtablissementCompensation;



}