package com.itma.gestionProjet.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class TacheResponseDTO {

    private Long id;
    private String libelle;
    private String description;
    private Date dateDebut;
    private Date dateFin;
    private String statut;
    private Integer progression;
}
