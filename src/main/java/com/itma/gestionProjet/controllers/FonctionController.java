package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.FonctionDto;
import com.itma.gestionProjet.requests.CategorieRequest;
import com.itma.gestionProjet.services.FonctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/fonctions")
public class FonctionController {

    @Autowired
    private FonctionService fonctionService;

    @PreAuthorize("@permissionChecker.has('FONCTIONS_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<FonctionDto>> getAllFonctions(Pageable pageable) {
        Page<FonctionDto> fonctions = fonctionService.getAllFonctions(pageable);
        return ResponseEntity.ok(new AApiResponse<>(200, fonctions.getContent(), pageable.getPageNumber(),
                pageable.getPageSize(), "Success", fonctions.getTotalElements()));
    }

    @PreAuthorize("@permissionChecker.has('FONCTIONS_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<AApiResponse<FonctionDto>> getFonctionById(@PathVariable Long id) {
        FonctionDto fonction = fonctionService.getFonctionById(id);
        return ResponseEntity.ok(new AApiResponse<>(200, Collections.singletonList(fonction), 0, 1, "Fonction found", 1));
    }

    @PreAuthorize("@permissionChecker.has('FONCTIONS_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<FonctionDto>> createFonction(@RequestBody CategorieRequest fonctionDto) {
        FonctionDto created = fonctionService.createFonction(fonctionDto);
        return ResponseEntity.ok(new AApiResponse<>(201, Collections.singletonList(created), 0, 1, "Fonction created", 1));
    }

    @PreAuthorize("@permissionChecker.has('FONCTIONS_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<FonctionDto>> updateFonction(@PathVariable Long id, @RequestBody CategorieRequest fonctionDto) {
        FonctionDto updated = fonctionService.updateFonction(id, fonctionDto);
        return ResponseEntity.ok(new AApiResponse<>(200, Collections.singletonList(updated), 0, 1, "Fonction updated", 1));
    }

    @PreAuthorize("@permissionChecker.has('FONCTIONS_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deleteFonction(@PathVariable Long id) {
        fonctionService.deleteFonction(id);
        return ResponseEntity.ok(new AApiResponse<>(200, null, 0, 0, "Fonction deleted", 0));
    }
}
