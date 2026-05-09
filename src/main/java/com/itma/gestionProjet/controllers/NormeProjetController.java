package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.ApiResponse;
import com.itma.gestionProjet.dtos.NormeProjetDTO;
import com.itma.gestionProjet.entities.NormeProjet;
import com.itma.gestionProjet.requests.NormeProjetRequest;
import com.itma.gestionProjet.services.imp.NormeProjetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/normes")
public class NormeProjetController {


    @Autowired
    NormeProjetService normeProjetService ;

    @PostMapping("/saveNorme/{projectId}")
    public NormeProjet saveNorme(@RequestBody NormeProjet normeProjet, @PathVariable Long projectId) {
        return normeProjetService.saveNormeProjet1(normeProjet, projectId);
    }

    @GetMapping("/all")
    public ApiResponse<List<NormeProjetDTO>> getAll() {
        // Récupérez toutes les normes sous forme de DTO
        List<NormeProjet> normes = normeProjetService.getAllNormeProjets();

        // Retournez les normes encapsulées dans un ApiResponse
        return new ApiResponse<>(200,"Liste des normes récupérée avec succès", normes);
    }

    @PostMapping(value = "/update/{projectId}")
    public List<NormeProjet> update(@RequestBody List<NormeProjet> normeProjects, @PathVariable Long projectId) {
        return normeProjetService.saveNormeProjet(normeProjects, projectId);
    }

    @GetMapping("/project/{projectId}")
    public List<NormeProjet> getByProject(@PathVariable Long projectId) {
        return normeProjetService.findByProjectId(projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        normeProjetService.deleteNormeProjetById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/updateNorme/{projectId}")
    public NormeProjet updateNorme(@RequestBody NormeProjet normeProjet, @PathVariable Long projectId) {
        return normeProjetService.updateSingleNorme(normeProjet);
    }
}
