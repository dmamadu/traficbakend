package com.itma.gestionProjet.dtos;

import com.itma.gestionProjet.entities.Quartier;
import lombok.Data;

@Data
public class QuartierDto {

    private Long id;
    private String nom;
    private Double latitudeCentre;
    private Double longitudeCentre;
    private Boolean actif;
    private int nombrePoints;

    public static QuartierDto from(Quartier quartier) {
        QuartierDto dto = new QuartierDto();
        dto.setId(quartier.getId());
        dto.setNom(quartier.getNom());
        dto.setLatitudeCentre(quartier.getLatitudeCentre());
        dto.setLongitudeCentre(quartier.getLongitudeCentre());
        dto.setActif(quartier.getActif());
        dto.setNombrePoints(
            quartier.getPointsReperes() != null ? quartier.getPointsReperes().size() : 0
        );
        return dto;
    }
}
