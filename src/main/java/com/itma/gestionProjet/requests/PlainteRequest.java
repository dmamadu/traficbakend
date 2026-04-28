package com.itma.gestionProjet.requests;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlainteRequest {

    // Identification
    private String statut;
    private String numeroReference;
    private LocalDate dateEnregistrement;
    private LocalDate moisReception;

    // PAP
    private String codePap;
    private String nomPrenom;
    private String mandataire;
    private String sexe;
    private String telephone;
    private String perimetreGmp;
    private String numeroParcelle;
    private String typeCarteIdentite;
    private String cin;
    private String typePap;
    private String villageQuartier;
    private String plainteParZone;

    // Plainte
    private String categorisation;
    private String objetPlainte;
    private String niveauGravite;
    private String descriptionPlainte;
    private String facilitateur;

    // Résolution
    private String descriptionReglement;
    private String observations;
    private String communicationResolution1;
    private LocalDate dateTraitementConsultant;
    private LocalDate dateVisite;
    private String communicationResolution2;
    private LocalDate dateTraitementClm;
    private String communicationResolution3;
    private LocalDate dateTraitementCcd;
    private String resolutionPlainte;
    private String siNonExpliquez;
    private String prochaineEtape;
    private LocalDate dateCloture;
    private String delaiResolution;

    // Projet
    private Long projectId;
}