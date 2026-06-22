package com.itma.gestionProjet.requests;

import lombok.Data;

@Data
public class QuartierRequest {

    private String nom;
    private Double latitudeCentre;
    private Double longitudeCentre;
}
