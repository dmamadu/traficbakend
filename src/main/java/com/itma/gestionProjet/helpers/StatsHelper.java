package com.itma.gestionProjet.helpers;

import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.CriterionStats;
import com.itma.gestionProjet.dtos.GlobalSummary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// StatsHelper.java
@Component
public class StatsHelper {

    private static final List<String> CRITERES = List.of(
            "Situation matrimoniale précaire",
            "Ménage avec personne handicapée",
            "Mineur chef de ménage",
            "Personne âgée sans soutien",
            "Ménage nombreux",
            "Analphabétisme"
    );

    private static final Set<String> MALE_TERMS   = Set.of("m", "masculin", "homme", "male", "h", "mâle");
    private static final Set<String> FEMALE_TERMS = Set.of("f", "féminin", "femme", "female", "fille", "feminin");

    // ← <T> ici c'est le seul changement par rapport à ta version
    public <T> CategoryStats buildCategoryStats(
            List<T> paps,
            Function<T, String> getSexe,
            Function<T, String> getVulnerabilite,
            double totalPerte
    ) {
        long total = paps.size();
        long hommes = 0, femmes = 0, autre = 0;
        long nonVulnerables = 0;
        long vulnHommes = 0, vulnFemmes = 0, vulnAutre = 0;

        Map<String, long[]> criteresCount = new LinkedHashMap<>();
        for (String critere : CRITERES) {
            criteresCount.put(critere, new long[]{0, 0, 0}); // [total, hommes, femmes]
        }

        for (T pap : paps) {  // ← T au lieu de Object
            String sexe = normalize(getSexe.apply(pap));
            String vuln = getVulnerabilite.apply(pap);

            String genderKey = getGenderKey(sexe);
            if ("Hommes".equals(genderKey)) hommes++;
            else if ("Femmes".equals(genderKey)) femmes++;
            else autre++;

            boolean isNonVuln = vuln == null || vuln.equals("Non vulnérable");
            if (isNonVuln) {
                nonVulnerables++;
            } else {
                for (String critere : CRITERES) {
                    if (vuln.contains(critere)) {
                        long[] counts = criteresCount.get(critere);
                        counts[0]++;
                        if ("Hommes".equals(genderKey)) counts[1]++;
                        else if ("Femmes".equals(genderKey)) counts[2]++;
                    }
                }
                if ("Hommes".equals(genderKey)) vulnHommes++;
                else if ("Femmes".equals(genderKey)) vulnFemmes++;
                else vulnAutre++;
            }
        }

        long totalVulnerables = total - nonVulnerables;

        Map<String, CriterionStats> criteresStats = new LinkedHashMap<>();
        criteresCount.forEach((critere, counts) ->
                criteresStats.put(critere, new CriterionStats(
                        counts[0],
                        counts[1],
                        counts[2],
                        counts[0] - counts[1] - counts[2]  // autre
                ))
        );

        CategoryStats stats = new CategoryStats();
        stats.setTotal(total);
        stats.setHommes(hommes);
        stats.setFemmes(femmes);
        stats.setAutre(autre);
        stats.setPercentHommes(total > 0 ? (hommes * 100.0 / total) : 0);
        stats.setPercentFemmes(total > 0 ? (femmes * 100.0 / total) : 0);
        stats.setTotalVulnerables(totalVulnerables);
        stats.setVulnerablesHommes(vulnHommes);
        stats.setVulnerablesFemmes(vulnFemmes);
        stats.setVulnerablesAutre(vulnAutre);
        stats.setCriteresVulnerabilite(criteresStats);
        stats.setTotalPerte(totalPerte);

        return stats;
    }

    public GlobalSummary buildSummary(CategoryStats pa, CategoryStats ag, CategoryStats hab) {
        long totalPersonnes = pa.getTotal() + ag.getTotal() + hab.getTotal();
        long totalHommes    = pa.getHommes() + ag.getHommes() + hab.getHommes();
        long totalFemmes    = pa.getFemmes() + ag.getFemmes() + hab.getFemmes();

        GlobalSummary summary = new GlobalSummary();
        summary.setTotalPersonnesAffectees(totalPersonnes);
        summary.setTotalVulnerables(pa.getTotalVulnerables() + ag.getTotalVulnerables() + hab.getTotalVulnerables());
        summary.setPercentHommesGlobal(totalPersonnes > 0 ? (totalHommes * 100.0 / totalPersonnes) : 0);
        summary.setPercentFemmesGlobal(totalPersonnes > 0 ? (totalFemmes * 100.0 / totalPersonnes) : 0);
        summary.setTotalCompensations(pa.getTotalPerte() + ag.getTotalPerte() + hab.getTotalPerte());
        return summary;
    }

    private String getGenderKey(String sexe) {
        if (FEMALE_TERMS.contains(sexe)) return "Femmes";
        if (MALE_TERMS.contains(sexe))   return "Hommes";
        return "Autre";
    }

    private String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }
}