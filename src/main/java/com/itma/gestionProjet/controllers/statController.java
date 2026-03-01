//package com.itma.gestionProjet.controllers;
//
//import com.itma.gestionProjet.dtos.CombinedStatsResponse;
//import com.itma.gestionProjet.services.DatabasePapAgricoleService;
//import com.itma.gestionProjet.services.DatabasePapPlaceAffaireService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/stats")
//public class statController {
//    @Autowired
//    private DatabasePapPlaceAffaireService databasePapPlaceAffaireService;
//
//    @Autowired
//    private DatabasePapAgricoleService databasePapAgricoleService;
//
//
//
//@GetMapping("/combine")
//public CombinedStatsResponse getCombinedStats(@RequestParam(required = false) Long projectId) {
//    // Récupérer les stats existantes
//    Map<String, Object> placeAffaire = databasePapPlaceAffaireService.getVulnerabilityStats(projectId);
//    Map<String, Object> agricole = databasePapAgricoleService.getVulnerabilityStats(projectId);
//
//    // Ajouter les stats de pertes
//    placeAffaire.put("statsPertes", getPerteStats(databasePapPlaceAffaireService, projectId));
//    agricole.put("statsPertes", getPerteStats(databasePapAgricoleService, projectId));
//
//    // Créer la réponse combinée
//    CombinedStatsResponse response = new CombinedStatsResponse();
//    response.setPlaceAffaireStats(placeAffaire);
//    response.setAgricoleStats(agricole);
//    response.setTotalStats(calculateTotalStats(placeAffaire, agricole));
//
//    return response;
//}
//
//    private Map<String, Double> getPerteStats(Object service, Long projectId) {
//        Map<String, Double> stats = new HashMap<>();
//
//        if (service instanceof DatabasePapPlaceAffaireService) {
//            DatabasePapPlaceAffaireService placeAffaireService = (DatabasePapPlaceAffaireService) service;
//            Double total = placeAffaireService.calculateTotalPerte(projectId);
////            Double moyenne = placeAffaireService.calculateAveragePerte(projectId);
//
//            stats.put("totalPerte", total);
////            stats.put("moyennePerte", moyenne);
//        }
//        else if (service instanceof DatabasePapAgricoleService) {
//            DatabasePapAgricoleService agricoleService = (DatabasePapAgricoleService) service;
//            Double total = agricoleService.calculateTotalPerte(projectId);
////            Double moyenne = agricoleService.calculateAveragePerte(projectId);
//
//            stats.put("totalPerte", total);
////            stats.put("moyennePerte", moyenne);
//        }
//
//        return stats;
//    }
//
//    private Map<String, Object> calculateTotalStats(Map<String, Object> stats1, Map<String, Object> stats2) {
//        Map<String, Object> totals = new HashMap<>();
//
//        // Calcul pour Vulnerabilites_globales
//        Map<String, Number> vulnGlobal1 = (Map<String, Number>) stats1.get("Vulnerabilites_globales");
//        Map<String, Number> vulnGlobal2 = (Map<String, Number>) stats2.get("Vulnerabilites_globales");
//        Map<String, Integer> vulnGlobalTotal = new HashMap<>();
//
//        vulnGlobal1.forEach((key, value) ->
//                vulnGlobalTotal.put(key, value.intValue() + vulnGlobal2.getOrDefault(key, 0).intValue()));
//
//        // Calcul pour Sexes_globaux
//        Map<String, Number> sexes1 = (Map<String, Number>) stats1.get("Sexes_globaux");
//        Map<String, Number> sexes2 = (Map<String, Number>) stats2.get("Sexes_globaux");
//        Map<String, Integer> sexesTotal = new HashMap<>();
//
//        sexes1.forEach((key, value) ->
//                sexesTotal.put(key, value.intValue() + sexes2.getOrDefault(key, 0).intValue()));
//
//        // Calcul pour Vulnerabilites_par_sexe
//        Map<String, Map<String, Number>> vulnSexe1 = (Map<String, Map<String, Number>>) stats1.get("Vulnerabilites_par_sexe");
//        Map<String, Map<String, Number>> vulnSexe2 = (Map<String, Map<String, Number>>) stats2.get("Vulnerabilites_par_sexe");
//        Map<String, Map<String, Integer>> vulnSexeTotal = new HashMap<>();
//
//        vulnSexe1.forEach((vulnKey, sexeMap) -> {
//            Map<String, Integer> totalSexeMap = new HashMap<>();
//            sexeMap.forEach((sexeKey, value) -> {
//                int sum = value.intValue() + vulnSexe2.get(vulnKey).get(sexeKey).intValue();
//                totalSexeMap.put(sexeKey, sum);
//            });
//            vulnSexeTotal.put(vulnKey, totalSexeMap);
//        });
//
//        // Construire la réponse totale
//        totals.put("Vulnerabilites_globales", vulnGlobalTotal);
//        totals.put("Sexes_globaux", sexesTotal);
//        totals.put("Vulnerabilites_par_sexe", vulnSexeTotal);
//
//        return totals;
//    }
//}

package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.CombinedStatsResponse;
import com.itma.gestionProjet.dtos.CriterionStats;
import com.itma.gestionProjet.helpers.StatsHelper;
import com.itma.gestionProjet.services.DatabasePapAgricoleService;
import com.itma.gestionProjet.services.DatabasePapPlaceAffaireService;
import com.itma.gestionProjet.services.DatabasePapHabitatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class statController {
    @Autowired
    private DatabasePapPlaceAffaireService databasePapPlaceAffaireService;

    @Autowired
    private DatabasePapAgricoleService databasePapAgricoleService;

    @Autowired
    private DatabasePapHabitatService databasePapHabitatService;


    @Autowired

    private  StatsHelper statsHelper;
    @Autowired

    private  DatabasePapPlaceAffaireService placeAffaireService;
    @Autowired

    private  DatabasePapAgricoleService agricoleService;
    @Autowired

    private  DatabasePapHabitatService habitatService;




//    @GetMapping("/combine")
//    public CombinedStatsResponse getCombinedStats(@RequestParam(required = false) Long projectId) {
//        // Récupérer les stats existantes
//        Map<String, Object> placeAffaire = databasePapPlaceAffaireService.getVulnerabilityStats(projectId);
//        Map<String, Object> agricole = databasePapAgricoleService.getVulnerabilityStats(projectId);
//        Map<String, Object> habitat = databasePapHabitatService.getVulnerabilityStats(projectId);
//
//        // Ajouter les stats de pertes
//        placeAffaire.put("statsPertes", getPerteStats(databasePapPlaceAffaireService, projectId));
//        agricole.put("statsPertes", getPerteStats(databasePapAgricoleService, projectId));
//        habitat.put("statsPertes", getPerteStats(databasePapHabitatService, projectId));
//
//        // Créer la réponse combinée
//        CombinedStatsResponse response = new CombinedStatsResponse();
//        response.setPlaceAffaireStats(placeAffaire);
//        response.setAgricoleStats(agricole);
//        response.setHabitatStats(habitat);
//        response.setTotalStats(calculateTotalStats(placeAffaire, agricole, habitat));
//
//        return response;
//    }
    @GetMapping("/combine")
    public CombinedStatsResponse getCombinedStats(@RequestParam(required = false) Long projectId) {

        CategoryStats paStats  = placeAffaireService.getCategoryStats(projectId);
        CategoryStats agStats  = agricoleService.getCategoryStats(projectId);
        CategoryStats habStats = habitatService.getCategoryStats(projectId);

        // Total = addition des 3
        CategoryStats totalStats = buildTotal(paStats, agStats, habStats);

        CombinedStatsResponse response = new CombinedStatsResponse();
        response.setPlaceAffaireStats(paStats);
        response.setAgricoleStats(agStats);
        response.setHabitatStats(habStats);
        response.setTotalStats(totalStats);
        response.setSummary(statsHelper.buildSummary(paStats, agStats, habStats));

        return response;
    }

    private CategoryStats buildTotal(CategoryStats... stats) {
        CategoryStats total = new CategoryStats();
        total.setTotal(Arrays.stream(stats).mapToLong(CategoryStats::getTotal).sum());
        total.setHommes(Arrays.stream(stats).mapToLong(CategoryStats::getHommes).sum());
        total.setFemmes(Arrays.stream(stats).mapToLong(CategoryStats::getFemmes).sum());
        total.setAutre(Arrays.stream(stats).mapToLong(CategoryStats::getAutre).sum());
        total.setTotalVulnerables(Arrays.stream(stats).mapToLong(CategoryStats::getTotalVulnerables).sum());
        total.setVulnerablesHommes(Arrays.stream(stats).mapToLong(CategoryStats::getVulnerablesHommes).sum());
        total.setVulnerablesFemmes(Arrays.stream(stats).mapToLong(CategoryStats::getVulnerablesFemmes).sum());
        total.setTotalPerte(Arrays.stream(stats).mapToDouble(CategoryStats::getTotalPerte).sum());
        long t = total.getTotal();
        total.setPercentHommes(t > 0 ? (total.getHommes() * 100.0 / t) : 0);
        total.setPercentFemmes(t > 0 ? (total.getFemmes() * 100.0 / t) : 0);
        // Critères : additionner clé par clé
        Map<String, CriterionStats> criteresTotal = new LinkedHashMap<>();
        stats[0].getCriteresVulnerabilite().forEach((key, val) -> {
            long th = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getHommes()).sum();
            long tf = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getFemmes()).sum();
            long ta = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getAutre()).sum();
            criteresTotal.put(key, new CriterionStats(th + tf + ta, th, tf, ta));
        });
        total.setCriteresVulnerabilite(criteresTotal);
        return total;
    }

    private Map<String, Double> getPerteStats(Object service, Long projectId) {
        Map<String, Double> stats = new HashMap<>();

        if (service instanceof DatabasePapPlaceAffaireService) {
            DatabasePapPlaceAffaireService placeAffaireService = (DatabasePapPlaceAffaireService) service;
            Double total = placeAffaireService.calculateTotalPerte(projectId);
            stats.put("totalPerte", total);
        }
        else if (service instanceof DatabasePapAgricoleService) {
            DatabasePapAgricoleService agricoleService = (DatabasePapAgricoleService) service;
            Double total = agricoleService.calculateTotalPerte(projectId);
            stats.put("totalPerte", total);
        }
        else if (service instanceof DatabasePapHabitatService) {
            DatabasePapHabitatService habitatService = (DatabasePapHabitatService) service;
            Double total = habitatService.calculateTotalPerte(projectId);
            stats.put("totalPerte", total);
        }

        return stats;
    }

    private Map<String, Object> calculateTotalStats(Map<String, Object> stats1, Map<String, Object> stats2, Map<String, Object> stats3) {
        Map<String, Object> totals = new HashMap<>();

        // Calcul pour Vulnerabilites_globales
        Map<String, Number> vulnGlobal1 = (Map<String, Number>) stats1.get("Vulnerabilites_globales");
        Map<String, Number> vulnGlobal2 = (Map<String, Number>) stats2.get("Vulnerabilites_globales");
        Map<String, Number> vulnGlobal3 = (Map<String, Number>) stats3.get("Vulnerabilites_globales");
        Map<String, Integer> vulnGlobalTotal = new HashMap<>();

        vulnGlobal1.forEach((key, value) ->
                vulnGlobalTotal.put(key, value.intValue() +
                        vulnGlobal2.getOrDefault(key, 0).intValue() +
                        vulnGlobal3.getOrDefault(key, 0).intValue()));

        // Calcul pour Sexes_globaux
        Map<String, Number> sexes1 = (Map<String, Number>) stats1.get("Sexes_globaux");
        Map<String, Number> sexes2 = (Map<String, Number>) stats2.get("Sexes_globaux");
        Map<String, Number> sexes3 = (Map<String, Number>) stats3.get("Sexes_globaux");
        Map<String, Integer> sexesTotal = new HashMap<>();

        sexes1.forEach((key, value) ->
                sexesTotal.put(key, value.intValue() +
                        sexes2.getOrDefault(key, 0).intValue() +
                        sexes3.getOrDefault(key, 0).intValue()));

        // Calcul pour Vulnerabilites_par_sexe
        Map<String, Map<String, Number>> vulnSexe1 = (Map<String, Map<String, Number>>) stats1.get("Vulnerabilites_par_sexe");
        Map<String, Map<String, Number>> vulnSexe2 = (Map<String, Map<String, Number>>) stats2.get("Vulnerabilites_par_sexe");
        Map<String, Map<String, Number>> vulnSexe3 = (Map<String, Map<String, Number>>) stats3.get("Vulnerabilites_par_sexe");
        Map<String, Map<String, Integer>> vulnSexeTotal = new HashMap<>();

        vulnSexe1.forEach((vulnKey, sexeMap) -> {
            Map<String, Integer> totalSexeMap = new HashMap<>();
            sexeMap.forEach((sexeKey, value) -> {
                int sum = value.intValue() +
                        vulnSexe2.get(vulnKey).get(sexeKey).intValue() +
                        vulnSexe3.get(vulnKey).get(sexeKey).intValue();
                totalSexeMap.put(sexeKey, sum);
            });
            vulnSexeTotal.put(vulnKey, totalSexeMap);
        });

        // Construire la réponse totale
        totals.put("Vulnerabilites_globales", vulnGlobalTotal);
        totals.put("Sexes_globaux", sexesTotal);
        totals.put("Vulnerabilites_par_sexe", vulnSexeTotal);

        return totals;
    }
}