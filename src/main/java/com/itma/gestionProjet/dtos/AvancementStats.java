package com.itma.gestionProjet.dtos;

import lombok.Data;

@Data
public class AvancementStats {
    private long brouillon;
    private long synchronisee;
    private long finalisee;
    private long compensationAEtablir;
    private long papAInformer;
    private long accordAObtenir;
    private long paiementAEffectuer;
    private long formationADonner;
    private long suiviAEffectuer;
    private long processusTermine;
}
