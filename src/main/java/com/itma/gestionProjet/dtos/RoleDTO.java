package com.itma.gestionProjet.dtos;

import lombok.Data;

import java.util.List;

@Data
public class RoleDTO {

    private Long id;
    private String name;
    private List<String> permissionCodes;

}
