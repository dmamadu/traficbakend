package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.UserProjectRoleDTO;
import com.itma.gestionProjet.requests.UserProjectRoleRequest;
import com.itma.gestionProjet.services.UserProjectRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-project-roles")
public class UserProjectRoleController {

    @Autowired
    private UserProjectRoleService userProjectRoleService;

    @GetMapping("/user/{userId}")
    public AApiResponse<UserProjectRoleDTO> listByUser(@PathVariable Long userId) {
        List<UserProjectRoleDTO> list = userProjectRoleService.listByUser(userId);
        return new AApiResponse<>(200, list, 0, list.size(), "Affectations récupérées avec succès", list.size());
    }

    @PreAuthorize("@permissionChecker.has('UTILISATEURS_MODIFIER')")
    @PostMapping
    public AApiResponse<UserProjectRoleDTO> assign(@RequestBody UserProjectRoleRequest request) {
        UserProjectRoleDTO dto = userProjectRoleService.assign(request.getUserId(), request.getProjectId(), request.getRoleId());
        return new AApiResponse<>(200, List.of(dto), 0, 1, "Rôle affecté avec succès", 1);
    }

    @PreAuthorize("@permissionChecker.has('UTILISATEURS_MODIFIER')")
    @DeleteMapping("/{id}")
    public AApiResponse<Void> remove(@PathVariable Long id) {
        userProjectRoleService.remove(id);
        return new AApiResponse<>(200, null, 0, 1, "Affectation supprimée avec succès", 1);
    }
}
