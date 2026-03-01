package com.itma.gestionProjet.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

// CriterionStats.java
@Data
@AllArgsConstructor
public class CriterionStats {
    private long total;
    private long hommes;
    private long femmes;
    private long autre;
}
