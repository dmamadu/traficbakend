package com.itma.gestionProjet.requests;

import com.itma.gestionProjet.entities.PointRepereType;
import lombok.Data;

@Data
public class PointRepereRequest {

    private String nom;
    private PointRepereType type;
    private Long quartierId;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String description;
}
