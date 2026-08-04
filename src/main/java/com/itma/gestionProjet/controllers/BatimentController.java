package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.BatimentDto;
import com.itma.gestionProjet.entities.Batiment;
import com.itma.gestionProjet.requests.RequestBatiment;
import com.itma.gestionProjet.services.BatimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/batiments")
public class BatimentController {

    @Autowired
    private BatimentService batimentService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public AApiResponse<Batiment> getAllBatiments(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                                  @RequestParam(value = "max", defaultValue = "10000") int max,
                                                  @RequestParam(required = false) Long projectId) {
        return batimentService.getAllBatiments(offset, max);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<List<Batiment>>> createBatiments(@RequestBody List<RequestBatiment> requests, @RequestParam Long projectId) {
        AApiResponse<List<Batiment>> response = new AApiResponse<>();
        try {
            Batiment batimentDtos =  batimentService.createBatiment(requests);
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(Collections.singletonList(Collections.singletonList(batimentDtos)));
            response.setMessage("Bâtiments créés avec succès");
            response.setLength(0);
            response.setOffset(0);
            response.setMax(requests.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Erreur lors de la création des bâtiments : " + e.getMessage());
            response.setData(null);
            response.setLength(0);
            response.setOffset(0);
            response.setMax(0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<Batiment> getBatimentById(@PathVariable Long id, @RequestParam Long projectId) {
        return batimentService.getBatimentById(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public AApiResponse<Batiment> updateBatiment(@PathVariable Long id, @RequestBody Batiment batiment, @RequestParam Long projectId) {
        return batimentService.updateBatiment(id, batiment);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<Void> deleteBatiment(@PathVariable Long id, @RequestParam Long projectId) {
        return batimentService.deleteBatiment(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<List<Batiment>>> getBatimentsByCodePap(@RequestParam String codePap,
                                                                              @RequestParam(defaultValue = "0") int offset,
                                                                              @RequestParam(defaultValue = "100") int max,
                                                                              @RequestParam Long projectId) {
        AApiResponse<List<Batiment>> response = batimentService.getBatimentsByCodePap(codePap, offset, max);

        if (response.getData() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }
}