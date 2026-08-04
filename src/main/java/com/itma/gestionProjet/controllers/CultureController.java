package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.CultureDTO;
import com.itma.gestionProjet.entities.Coproprietaire;
import com.itma.gestionProjet.entities.Culture;
import com.itma.gestionProjet.requests.CultureRequest;
import com.itma.gestionProjet.services.CultureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/cultures")
@Validated
public class CultureController {

    @Autowired
    private CultureService cultureService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping()
    public AApiResponse<Culture> getAllCultures(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int max,
            @RequestParam(required = false) Long projectId) {
        return cultureService.getAllCultures(offset, max);
    }
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping()
    public ResponseEntity<AApiResponse<List<CultureDTO>>> createCulture(@Valid @RequestBody List<CultureRequest> cultureRequests, @RequestParam Long projectId) {
        AApiResponse<List<CultureDTO>> response = new AApiResponse<>();
        try {
            List<CultureDTO> cultureDTOs = cultureService.createCulture(cultureRequests);
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(Collections.singletonList(cultureDTOs));
            response.setMessage("Cultures créées avec succès");
            response.setLength(cultureDTOs.size());
            response.setOffset(0); // Vous pouvez ajuster l'offset si nécessaire
            response.setMax(cultureRequests.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Erreur lors de la création des cultures : " + e.getMessage());
            response.setData(Collections.emptyList());
            response.setLength(0);
            response.setOffset(0);
            response.setMax(cultureRequests.size());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<List<Culture>>> getCulturesByCodePap(@RequestParam String codePap,
                                                                            @RequestParam(defaultValue = "0") int offset,
                                                                            @RequestParam(defaultValue = "10") int max,
                                                                            @RequestParam Long projectId) {
        AApiResponse<List<Culture>> response = cultureService.getCulturesByCodePap(codePap, offset, max);

        if (response.getData() == null) {
            return ResponseEntity.status(response.getResponseCode()).body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }
}
