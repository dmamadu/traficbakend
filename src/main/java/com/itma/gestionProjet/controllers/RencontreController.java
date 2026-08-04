package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.RencontreDTO;
import com.itma.gestionProjet.dtos.TacheDTO;
import com.itma.gestionProjet.services.RencontreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/rencontres")
public class RencontreController {


    @Autowired
    private RencontreService rencontreService;

    // Récupérer les rencontres d'un projet
    @PreAuthorize("@permissionChecker.has(#projectId, 'ELABORATION_PAR_VOIR')")
    @GetMapping("/project/{projectId}")
    public AApiResponse<RencontreDTO> getRencontresByProject(
            @PathVariable Long projectId,
            Pageable pageable) {

        Page<RencontreDTO> rencontresPage = rencontreService.getRencontresByProjectId(projectId, pageable);

        AApiResponse<RencontreDTO> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.OK.value());
        response.setData(rencontresPage.getContent());
        response.setOffset((int) rencontresPage.getPageable().getOffset());
        response.setMax(rencontresPage.getPageable().getPageSize());
        response.setLength(rencontresPage.getTotalElements());
        response.setMessage("Success");

        return response;
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'ELABORATION_PAR_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<RencontreDTO>> getRencontresByProject(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max, @RequestParam(required = false) Long projectId) {
        PageRequest pageRequest = PageRequest.of(offset, max);
        Page<RencontreDTO> rencontresPages;
        if (projectId != null) {
            rencontresPages = rencontreService.getRencontresByProjectId(projectId, pageRequest);
        } else {
            rencontresPages = rencontreService.getAllRencontres(pageRequest);
        }
        AApiResponse<RencontreDTO> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setData(rencontresPages.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(rencontresPages.getTotalElements());
        return ResponseEntity.ok().body(response);
    }


    // Créer une nouvelle rencontre
    @PreAuthorize("@permissionChecker.has(#rencontreDTO.projectId, 'ELABORATION_PAR_CREER')")
    @PostMapping
    public AApiResponse<RencontreDTO> createRencontre(@RequestBody RencontreDTO rencontreDTO) {
        RencontreDTO createdRencontre = rencontreService.createRencontre(rencontreDTO);
        AApiResponse<RencontreDTO> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.CREATED.value());
        response.setData(List.of(createdRencontre));
        response.setOffset(0);
        response.setMax(1);
        response.setLength(1);
        response.setMessage("Rencontre created successfully");

        return response;
    }

    // Mettre à jour une rencontre existante
    @PreAuthorize("@permissionChecker.has(#rencontreDTO.projectId, 'ELABORATION_PAR_MODIFIER')")
    @PutMapping("/{id}")
    public AApiResponse<RencontreDTO> updateRencontre(@PathVariable Long id, @RequestBody RencontreDTO rencontreDTO) {
        RencontreDTO updatedRencontre = rencontreService.updateRencontre(id, rencontreDTO);

        AApiResponse<RencontreDTO> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.OK.value());
        response.setData(List.of(updatedRencontre));
        response.setOffset(0);
        response.setMax(1);
        response.setLength(1);
        response.setMessage("Rencontre updated successfully");

        return response;
    }

    // Supprimer une rencontre
    @PreAuthorize("@permissionChecker.has(#projectId, 'ELABORATION_PAR_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<Void> deleteRencontre(@PathVariable Long id, @RequestParam Long projectId) {
        rencontreService.deleteRencontre(id);

        AApiResponse<Void> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.NO_CONTENT.value());
        response.setData(null);
        response.setOffset(0);
        response.setMax(0);
        response.setLength(0);
        response.setMessage("Rencontre deleted successfully");

        return response;
    }

}
