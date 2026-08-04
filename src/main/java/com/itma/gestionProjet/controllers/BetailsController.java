package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.BetailsDTO;
import com.itma.gestionProjet.entities.Betails;
import com.itma.gestionProjet.requests.BetailsRequest;
import com.itma.gestionProjet.services.BetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/betails")
public class BetailsController {

    @Autowired
    private BetailsService betailsService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping()
    public ResponseEntity<AApiResponse<List<BetailsDTO>>> createBetails(@RequestBody List<BetailsRequest> requests, @RequestParam Long projectId) {
        AApiResponse<List<BetailsDTO>> response = new AApiResponse<>();
        try {
            List<BetailsDTO> betailsDTOs = (List<BetailsDTO>) betailsService.createBetails(requests);
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(Collections.singletonList(betailsDTOs));
            response.setMessage("Betails créés avec succès");
            response.setLength(betailsDTOs.size());
            response.setOffset(0);
            response.setMax(requests.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Erreur lors de la création des Betails : " + e.getMessage());
            response.setData(null);
            response.setLength(0);
            response.setOffset(0);
            response.setMax(0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public AApiResponse<Betails> getAllBetails(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Long projectId) {
        return betailsService.getAllBetails(page, size);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<Betails> getBetailsById(@PathVariable Long id, @RequestParam Long projectId) {
        return betailsService.getBetailsById(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public AApiResponse<Betails> updateBetails(@PathVariable Long id, @RequestBody Betails betails, @RequestParam Long projectId) {
        return betailsService.updateBetails(id, betails);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<Betails> deleteBetails(@PathVariable Long id, @RequestParam Long projectId) {
        return betailsService.deleteBetails(id);
    }




    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<List<Betails>>> getBetailsByCodePap(@RequestParam String codePap,
                                                                           @RequestParam(defaultValue = "0") int offset,
                                                                           @RequestParam(defaultValue = "10") int max,
                                                                           @RequestParam Long projectId) {
        AApiResponse<List<Betails>> response = betailsService.getBetailsByCodePap(codePap, offset, max);

        if (response.getData() == null) {
            return ResponseEntity.status(response.getResponseCode()).body(response);
        } else {
            return ResponseEntity.ok(response);
        }
    }
}
