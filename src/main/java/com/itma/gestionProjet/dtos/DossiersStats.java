package com.itma.gestionProjet.dtos;

import lombok.Data;

@Data
public class DossiersStats {
    /** Ententes dont le statut est FINALISEE (accord signé). */
    private long ententesSignees;
    /** Ententes en attente de paiement (etatProcessus = PAIEMENT_A_EFFECTUER). */
    private long dossiersTransmis;
    /** PAP dont le paiement est effectué (etatProcessus in FORMATION_A_DONNER, SUIVI_A_EFFECTUER, PROCESSUS_TERMINE). */
    private long papPayees;
}
