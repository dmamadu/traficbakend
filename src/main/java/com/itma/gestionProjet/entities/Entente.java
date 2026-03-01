package com.itma.gestionProjet.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "entente")
public class Entente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private StatutEntente statut;

    @Enumerated(EnumType.STRING)
    private EtatProcessusEntente etatProcessus;

    // Référence vers le PAP
    private Long papId;

    private String codePap; // Ajoutez ce champ

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String detailsInformation;

    @Enumerated(EnumType.STRING)
    private TypePap papType;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Dates importantes
    private LocalDateTime dateCreation;
    private LocalDateTime dateSynchronisation;
    private LocalDateTime dateFinalisation;

    // --- PROCESSUS DE VALIDATION (6 ÉTAPES) ---

    // Étape 1: Établissement de la compensation
    private Boolean compensationEtablie;
    private LocalDateTime dateEtablissementCompensation;

    // Étape 2: Information de la PAP
    private Boolean papInformee;
    private LocalDateTime dateInformationPap;
    private String modeInformation;

    // Étape 3: Accord de la PAP
    private Boolean accordPapObtenu;
    private LocalDateTime dateAccordPap;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String preuveAccord;

    // Étape 4: Paiement effectué
    private Boolean paiementEffectue;
    private LocalDateTime datePaiement;
    private String optionPaiement;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String referencePaiement;

    // Étape 5: Formation/Encadrement
    private Boolean formationDonnee;
    private LocalDateTime dateFormation;
    private String typeFormation;
    private String formateur;

    // Étape 6: Suivi post-paiement
    private Boolean suiviEffectue;
    private LocalDateTime dateSuivi;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resultatSuivi;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String commentairesSuivi;

    // Signatures
    private String urlSignaturePap;
    private String urlSignatureResponsable;





    // Historique des modifications
    @ElementCollection
    @CollectionTable(name = "entente_modifications", joinColumns = @JoinColumn(name = "entente_id"))
    @MapKeyColumn(name = "modification_key")
    @Column(name = "modification_value", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> historiqueModifications = new HashMap<>();



    public void ajouterModification(String champ, String ancienneValeur, String nouvelleValeur, String utilisateur) {
        String modification = String.format("%s: %s -> %s (par %s à %s)",
                champ, ancienneValeur, nouvelleValeur, utilisateur, LocalDateTime.now());
        historiqueModifications.put(LocalDateTime.now().toString(), modification);
    }









    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        statut = StatutEntente.BROUILLON;
        etatProcessus = EtatProcessusEntente.COMPENSATION_A_ETABLIR;

        // Initialisation des booléens
        compensationEtablie = false;
        papInformee = false;
        accordPapObtenu = false;
        paiementEffectue = false;
        formationDonnee = false;
        suiviEffectue = false;
    }
}