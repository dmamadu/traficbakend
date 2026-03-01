package com.itma.gestionProjet.dtos;

import lombok.Data;

import java.util.Map;

// CategoryStats.java
@Data
public class CategoryStats {
    // Sexes
    private long total;
    private long hommes;
    private long femmes;
    private long autre;
    private double percentHommes;
    private double percentFemmes;

    // Vulnérables (déjà calculés : total - nonVulnerable)
    private long totalVulnerables;
    private long vulnerablesHommes;
    private long vulnerablesFemmes;
    private long vulnerablesAutre;

    // Critères détaillés
    private Map<String, CriterionStats> criteresVulnerabilite;

    // Pertes
    private double totalPerte;
}
