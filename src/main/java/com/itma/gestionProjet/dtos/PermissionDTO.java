package com.itma.gestionProjet.dtos;

import lombok.Data;

@Data
public class PermissionDTO {
    private Long id;
    private String code;
    private String libelle;
    private String module;
}
