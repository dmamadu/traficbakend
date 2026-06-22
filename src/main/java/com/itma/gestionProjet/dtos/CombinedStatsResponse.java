package com.itma.gestionProjet.dtos;

import lombok.Data;

@Data
public class CombinedStatsResponse {

    private CategoryStats placeAffaireStats;
    private CategoryStats agricoleStats;
    private CategoryStats habitatStats;
    private CategoryStats totalStats;
    private GlobalSummary summary;
    private DossiersStats dossiersStats;
}