package com.itma.gestionProjet.dtos;

import com.itma.gestionProjet.entities.PointRepere;
import com.itma.gestionProjet.entities.PointRepereType;
import lombok.Data;

@Data
public class PointRepereDto {

    private Long id;
    private String nom;
    private PointRepereType type;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String description;
    private Boolean actif;
    private QuartierInfo quartier;

    @Data
    public static class QuartierInfo {
        private Long id;
        private String nom;
        private Double latitudeCentre;
        private Double longitudeCentre;
    }

    public static PointRepereDto from(PointRepere point) {
        PointRepereDto dto = new PointRepereDto();
        dto.setId(point.getId());
        dto.setNom(point.getNom());
        dto.setType(point.getType());
        dto.setLatitude(point.getLatitude());
        dto.setLongitude(point.getLongitude());
        dto.setPhotoUrl(point.getPhotoUrl());
        dto.setDescription(point.getDescription());
        dto.setActif(point.getActif());

        if (point.getQuartier() != null) {
            QuartierInfo info = new QuartierInfo();
            info.setId(point.getQuartier().getId());
            info.setNom(point.getQuartier().getNom());
            info.setLatitudeCentre(point.getQuartier().getLatitudeCentre());
            info.setLongitudeCentre(point.getQuartier().getLongitudeCentre());
            dto.setQuartier(info);
        }

        return dto;
    }
}
