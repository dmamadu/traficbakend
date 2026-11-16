package com.itma.gestionProjet.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Une ligne de la liste « Dossiers à l'étape … » du tableau de bord M/O. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DossierEtapeDto {
    private Long ententeId;
    private String codePap;
    private String prenom;
    private String nom;
    private String categorie;
    private boolean vulnerable;
    private String zone;
    private Double montant;
    /** Jours écoulés depuis l'entrée dans l'étape actuelle (null si la date est inconnue). */
    private Long joursEtape;
}
