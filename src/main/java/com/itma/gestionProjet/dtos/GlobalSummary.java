package com.itma.gestionProjet.dtos;

import lombok.Data;

// GlobalSummary.java  ← tout ce dont le frontend a besoin pour les KPI
@Data
public class GlobalSummary {
    private long totalPersonnesAffectees;
    private long totalVulnerables;
    private double percentHommesGlobal;
    private double percentFemmesGlobal;
    private double totalCompensations;
}
