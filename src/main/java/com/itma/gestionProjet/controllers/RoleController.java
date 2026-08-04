package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.RoleDTO;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.requests.RoleRequest;
import com.itma.gestionProjet.services.imp.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/roles")
public class RoleController {

    RoleService roleService;
    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PreAuthorize("@permissionChecker.has('ROLES_VOIR')")
    @RequestMapping(path = "/all", method = RequestMethod.GET)
    public AApiResponse<Role> getRoles() {
        List<Role> roles = roleService.getAllRoles();
        return new AApiResponse<>(200, roles, 0, roles.size(), "Roles retrieved successfully", roles.size());
    }

    @PreAuthorize("@permissionChecker.has('ROLES_CREER')")
    @RequestMapping(path = "/createRole", method = RequestMethod.POST)
    public AApiResponse<RoleDTO> createRole(@RequestBody RoleRequest roleRequest) {
        RoleDTO createdRole = roleService.saveRole(roleRequest);
        return new AApiResponse<>(201, List.of(createdRole), 0, 1, "Role created successfully", 1);
    }

    @PreAuthorize("@permissionChecker.has('ROLES_MODIFIER')")
    @RequestMapping(path = "/updateRole/{id}", method = RequestMethod.PUT)
    public AApiResponse<RoleDTO> updateRole(@RequestBody RoleRequest roleRequest,@PathVariable Long id) {
        RoleDTO updatedRole = roleService.updateRole(id,roleRequest);
        return new AApiResponse<>(200, List.of(updatedRole), 0, 1, "Role updated successfully", 1);
    }

    @PreAuthorize("@permissionChecker.has('ROLES_SUPPRIMER')")
    @RequestMapping(path = "/deleteRole/{id}", method = RequestMethod.DELETE)
    public AApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRoleById(id);
        return new AApiResponse<>(200, null, 0, 1, "Role deleted successfully", 1);
    }

    @PreAuthorize("@permissionChecker.has('ROLES_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<RoleDTO> getRole(@PathVariable Long id) {
        RoleDTO role = roleService.getRole(id);
        return new AApiResponse<>(200, List.of(role), 0, 1, "Role retrieved successfully", 1);
    }

    /**
     * Remplace intégralement le jeu de permissions d'un rôle (matrice d'administration).
     * Réservé à ceux qui détiennent la méta-permission ROLES_GERER_PERMISSIONS — évite
     * qu'un rôle puisse librement s'auto-accorder des droits supplémentaires.
     */
    @PreAuthorize("@permissionChecker.has('ROLES_GERER_PERMISSIONS')")
    @PutMapping("/{id}/permissions")
    public AApiResponse<RoleDTO> setRolePermissions(@PathVariable Long id, @RequestBody List<String> permissionCodes) {
        try {
            RoleDTO role = roleService.setRolePermissions(id, permissionCodes);
            return new AApiResponse<>(200, List.of(role), 0, 1, "Permissions du rôle mises à jour", 1);
        } catch (Exception ex) {
            log.error(">>> ROLE_PERMISSIONS_ERROR roleId={} codes={}", id, permissionCodes, ex);
            throw ex;
        }
    }

}
