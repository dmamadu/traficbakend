package com.itma.gestionProjet.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plainte")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plainte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identification
    @Column(name = "statut")
    private String statut;

    @Column(name = "numero_reference", unique = true)
    private String numeroReference;

    @Column(name = "date_enregistrement")
    private LocalDate dateEnregistrement;

    @Column(name = "mois_reception")
    private LocalDate moisReception;

    // PAP (Personne Affectée par le Projet)
    @Column(name = "code_pap")
    private String codePap;

    @Column(name = "nom_prenom")
    private String nomPrenom;

    @Column(name = "mandataire")
    private String mandataire;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "perimetre_gmp")
    private String perimetreGmp;

    @Column(name = "numero_parcelle")
    private String numeroParcelle;

    @Column(name = "type_carte_identite")
    private String typeCarteIdentite;

    @Column(name = "cin")
    private String cin;

    @Column(name = "type_pap")
    private String typePap;

    @Column(name = "village_quartier")
    private String villageQuartier;

    @Column(name = "plainte_par_zone")
    private String plainteParZone;

    // Plainte
    @Column(name = "categorisation")
    private String categorisation;

    @Column(name = "objet_plainte", columnDefinition = "TEXT")
    private String objetPlainte;

    @Column(name = "niveau_gravite")
    private String niveauGravite;

    @Column(name = "description_plainte", columnDefinition = "TEXT")
    private String descriptionPlainte;

    @Column(name = "facilitateur")
    private String facilitateur;

    // Résolution
    @Column(name = "description_reglement", columnDefinition = "TEXT")
    private String descriptionReglement;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "communication_resolution1", columnDefinition = "TEXT")
    private String communicationResolution1;

    @Column(name = "date_traitement_consultant")
    private LocalDate dateTraitementConsultant;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    @Column(name = "communication_resolution2", columnDefinition = "TEXT")
    private String communicationResolution2;

    @Column(name = "date_traitement_clm")
    private LocalDate dateTraitementClm;

    @Column(name = "communication_resolution3", columnDefinition = "TEXT")
    private String communicationResolution3;

    @Column(name = "date_traitement_ccd")
    private LocalDate dateTraitementCcd;

    @Column(name = "resolution_plainte", columnDefinition = "TEXT")
    private String resolutionPlainte;

    @Column(name = "si_non_expliquez", columnDefinition = "TEXT")
    private String siNonExpliquez;

    @Column(name = "prochaine_etape", columnDefinition = "TEXT")
    private String prochaineEtape;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Column(name = "delai_resolution")
    private String delaiResolution;

    // Projet
    @Column(name = "project_id")
    private Long projectId;

    // Audit
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}