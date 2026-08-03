package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.PermissionDTO;
import com.itma.gestionProjet.entities.Permission;
import com.itma.gestionProjet.repositories.PermissionRepository;
import com.itma.gestionProjet.security.PermissionChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Catalogue en lecture seule des permissions (module x action). Le catalogue lui-même est figé
 * par PermissionSeeder — cet endpoint sert à peupler la future matrice d'administration
 * (Rôle x Permission), voir GESTION_PERMISSIONS_DOC.md §7.
 */
@RestController
@RequestMapping("/permissions")
public class PermissionController {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionChecker permissionChecker;

    /**
     * Permissions effectivement accordées à l'utilisateur connecté pour le projet actif —
     * consommé par le frontend (PermissionService) pour filtrer menu, routes et boutons.
     * Super Admin reçoit le catalogue complet (sentinel `null` de resolveGrantedCodes).
     */
    @GetMapping("/effective")
    public AApiResponse<String> getEffectivePermissions(@RequestParam(required = false) Long projectId) {
        Set<String> granted = permissionChecker.resolveGrantedCodes(projectId);
        List<String> codes = granted == null
                ? permissionRepository.findAll().stream().map(Permission::getCode).sorted().toList()
                : granted.stream().sorted().toList();
        return new AApiResponse<>(200, codes, 0, codes.size(), "Permissions effectives récupérées", codes.size());
    }

    @GetMapping("/all")
    public AApiResponse<PermissionDTO> getAll() {
        List<PermissionDTO> permissions = permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::getCode))
                .map(this::toDto)
                .toList();
        return new AApiResponse<>(200, permissions, 0, permissions.size(), "Permissions retrieved successfully", permissions.size());
    }

    private PermissionDTO toDto(Permission p) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setLibelle(p.getLibelle());
        dto.setModule(p.getModule());
        return dto;
    }
}
