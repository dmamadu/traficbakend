package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.EmployePap;
import com.itma.gestionProjet.entities.GeoPoly;
import com.itma.gestionProjet.requests.GeoPolyRequest;
import com.itma.gestionProjet.services.GeoPolyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/geopolys")
public class GeoPolyController {

    @Autowired
    private GeoPolyService geoPolyService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping()
    public ResponseEntity<AApiResponse<List<GeoPoly>>> createGeoPoly(@RequestBody List<GeoPolyRequest> geoPolyRequests, @RequestParam Long projectId) {
        AApiResponse<List<GeoPoly>> response = new AApiResponse<>();
        try {
            // Créer les objets GeoPoly
            List<GeoPoly> savedGeoPolys = (List<GeoPoly>) geoPolyService.createGeoPoly(geoPolyRequests);

            // Préparer la réponse
            response.setResponseCode(201);
            response.setData(Collections.singletonList(savedGeoPolys)); // Retirer le `Collections.singletonList`
            response.setMessage("GeoPolys créés avec succès");
            response.setLength(savedGeoPolys.size());
            response.setOffset(0);
            response.setMax(0);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Gérer les erreurs
            response.setResponseCode(500);
            response.setMessage("Erreur lors de la création des GeoPolys : " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }




    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public AApiResponse<GeoPoly> getAllGeoPolys(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Long projectId) {
        return geoPolyService.getAllGeoPolys(page, size);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<GeoPoly> getGeoPolyById(@PathVariable Long id, @RequestParam Long projectId) {
        return geoPolyService.getGeoPolyById(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public AApiResponse<GeoPoly> updateGeoPoly(@PathVariable Long id, @RequestBody GeoPoly geoPoly, @RequestParam Long projectId) {
        return geoPolyService.updateGeoPoly(id, geoPoly);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<GeoPoly> deleteGeoPoly(@PathVariable Long id, @RequestParam Long projectId) {
        return geoPolyService.deleteGeoPoly(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<List<GeoPoly>>> getGeoPolyByCodePap(@RequestParam String codePap,
                                                                               @RequestParam(defaultValue = "0") int offset,
                                                                               @RequestParam(defaultValue = "100") int max,
                                                                               @RequestParam Long projectId) {
        AApiResponse<List<GeoPoly>> response = geoPolyService.getGeoPolyByCodePap(codePap, offset, max);
        if (response.getData() == null) {
            return ResponseEntity.status(response.getResponseCode()).body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }
}