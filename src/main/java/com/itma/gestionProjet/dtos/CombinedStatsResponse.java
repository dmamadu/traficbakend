package com.itma.gestionProjet.dtos;

import lombok.Data;

import java.util.Map;

//@Data
//public class CombinedStatsResponse {
//    private Map<String, Object> placeAffaireStats;
//    private Map<String, Object> agricoleStats;
//    private Map<String, Object> habitatStats;
//    private Map<String, Object> totalStats;
//    private GlobalSummary summary;
//
//
//
//}


// CombinedStatsResponse.java
@Data
public class CombinedStatsResponse {

    private CategoryStats placeAffaireStats;
    private CategoryStats agricoleStats;
    private CategoryStats habitatStats;
    private CategoryStats totalStats;
    private GlobalSummary summary; // ← nouveau : résumé global prêt à l'emploi
}