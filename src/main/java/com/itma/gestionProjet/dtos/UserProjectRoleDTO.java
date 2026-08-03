package com.itma.gestionProjet.dtos;

import lombok.Data;

@Data
public class UserProjectRoleDTO {
    private Long id;
    private Long userId;
    private Long projectId;       // null = valable sur tous les projets de l'utilisateur
    private String projectLibelle; // null si projectId est null
    private Long roleId;
    private String roleName;
}
