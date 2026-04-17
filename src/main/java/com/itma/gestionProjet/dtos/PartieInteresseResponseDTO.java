package com.itma.gestionProjet.dtos;


import lombok.Data;

import java.util.List;

@Data
public class PartieInteresseResponseDTO {
    private Long id;
    private String adresse;
    private String courrielPrincipal;
    private String libelle;
    private String categorie;
    private String localisation;
//    private String normes;
    private String statut;
    private Long project_id;

    // Nouveau champ tableau pour les contacts
    private List<ContactDTO> contacts;
}



