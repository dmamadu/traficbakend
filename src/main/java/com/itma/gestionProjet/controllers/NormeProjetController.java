package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.ApiResponse;
import com.itma.gestionProjet.dtos.NormeProjetDTO;
import com.itma.gestionProjet.entities.NormeProjet;
import com.itma.gestionProjet.requests.NormeProjetRequest;
import com.itma.gestionProjet.services.imp.NormeProjetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Regroupé sous le module PROJETS (voir GESTION_PERMISSIONS_DOC.md §4 : "Liste + Normes,
// regroupés, ce sont deux vues du même objet").
@RestController
@RequestMapping("/normes")
public class NormeProjetController {


    @Autowired
    NormeProjetService normeProjetService ;

    @PreAuthorize("@permissionChecker.has('PROJETS_MODIFIER')")
    @PostMapping("/saveNorme/{projectId}")
    public NormeProjet saveNorme(@RequestBody NormeProjet normeProjet, @PathVariable Long projectId) {
        return normeProjetService.saveNormeProjet1(normeProjet, projectId);
    }

    @PreAuthorize("@permissionChecker.has('PROJETS_VOIR')")
    @GetMapping("/all")
    public ApiResponse<List<NormeProjetDTO>> getAll() {
        // Récupérez toutes les normes sous forme de DTO
        List<NormeProjet> normes = normeProjetService.getAllNormeProjets();

        // Retournez les normes encapsulées dans un ApiResponse
        return new ApiResponse<>(200,"Liste des normes récupérée avec succès", normes);
    }

    @PreAuthorize("@permissionChecker.has('PROJETS_MODIFIER')")
    @PostMapping(value = "/update/{projectId}")
    public List<NormeProjet> update(@RequestBody List<NormeProjet> normeProjects, @PathVariable Long projectId) {
        return normeProjetService.saveNormeProjet(normeProjects, projectId);
    }

    @PreAuthorize("@permissionChecker.has('PROJETS_VOIR')")
    @GetMapping("/project/{projectId}")
    public List<NormeProjet> getByProject(@PathVariable Long projectId) {
        return normeProjetService.findByProjectId(projectId);
    }

    @PreAuthorize("@permissionChecker.has('PROJETS_MODIFIER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        normeProjetService.deleteNormeProjetById(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@permissionChecker.has('PROJETS_MODIFIER')")
    @PutMapping("/updateNorme/{projectId}")
    public NormeProjet updateNorme(@RequestBody NormeProjet normeProjet, @PathVariable Long projectId) {
        return normeProjetService.updateSingleNorme(normeProjet);
    }
}
