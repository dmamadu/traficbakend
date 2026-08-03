package com.itma.gestionProjet.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProjectRoleRequest {
    private Long userId;
    private Long projectId; // null = valable sur tous les projets de l'utilisateur
    private Long roleId;
}
