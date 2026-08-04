package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.DescriptionEquipement;
import com.itma.gestionProjet.requests.DescriptionEquipementRequest;
import com.itma.gestionProjet.services.DescriptionEquipementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/equipements")
public class DescriptionEquipementController {

    @Autowired
    private DescriptionEquipementService descriptionEquipementService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<List<DescriptionEquipement>>> createDescriptionEquipement(@RequestBody List<DescriptionEquipementRequest> descriptionEquipementRequests, @RequestParam Long projectId) {
        AApiResponse<List<DescriptionEquipement>> response = new AApiResponse<>();
        try {
            List<DescriptionEquipement> descriptionEquipements = (List<DescriptionEquipement>) descriptionEquipementService.createDescriptionEquipement(descriptionEquipementRequests);
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(Collections.singletonList(descriptionEquipements));
            response.setMessage("Équipements créés avec succès");
            response.setLength(descriptionEquipements.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Erreur lors de la création des équipements : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public AApiResponse<DescriptionEquipement> getAllEquipements(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size,
                                                                 @RequestParam(required = false) Long projectId) {
        return descriptionEquipementService.getAllDescriptionEquipements(page, size);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<DescriptionEquipement> getEquipementById(@PathVariable Long id, @RequestParam Long projectId) {
        return descriptionEquipementService.getDescriptionEquipementById(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public AApiResponse<DescriptionEquipement> updateEquipement(@PathVariable Long id, @RequestBody DescriptionEquipement descriptionEquipement, @RequestParam Long projectId) {
        return descriptionEquipementService.updateDescriptionEquipement(id, descriptionEquipement);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<DescriptionEquipement> deleteEquipement(@PathVariable Long id, @RequestParam Long projectId) {
        return descriptionEquipementService.deleteDescriptionEquipement(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<List<DescriptionEquipement>>> getDescriptionByCodePap(@RequestParam String codePap,
                                                                                             @RequestParam(defaultValue = "0") int offset,
                                                                                             @RequestParam(defaultValue = "100") int max,
                                                                                             @RequestParam Long projectId) {
        AApiResponse<List<DescriptionEquipement>> response = descriptionEquipementService.getEquipementByCodePap(codePap, offset, max);

        if (response.getData() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }
}