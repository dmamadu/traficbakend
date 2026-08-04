package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.CategorieDto;
import com.itma.gestionProjet.services.CategorieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/categories")
public class CategorieController {

    @Autowired
    private CategorieService categorieService;

    @PreAuthorize("@permissionChecker.has('CATEGORIES_UTILISATEURS_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<CategorieDto>> getAllCategories(Pageable pageable) {
        Page<CategorieDto> categories = categorieService.getAllCategories(pageable);
        return ResponseEntity.ok(new AApiResponse<>(200, categories.getContent(), pageable.getPageNumber(),
                pageable.getPageSize(), "Success", categories.getTotalElements()));
    }

    @PreAuthorize("@permissionChecker.has('CATEGORIES_UTILISATEURS_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<AApiResponse<CategorieDto>> getCategorieById(@PathVariable Long id) {
        CategorieDto categorie = categorieService.getCategorieById(id);
        return ResponseEntity.ok(new AApiResponse<>(200, Collections.singletonList(categorie), 0, 1, "Categorie found", 1));
    }

    @PreAuthorize("@permissionChecker.has('CATEGORIES_UTILISATEURS_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<CategorieDto>> createCategorie(@RequestBody CategorieDto categorieDto) {
        CategorieDto created = categorieService.createCategorie(categorieDto);
        return ResponseEntity.ok(new AApiResponse<>(201, Collections.singletonList(created), 0, 1, "Categorie created", 1));
    }

    @PreAuthorize("@permissionChecker.has('CATEGORIES_UTILISATEURS_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<CategorieDto>> updateCategorie(@PathVariable Long id, @RequestBody CategorieDto categorieDto) {
        CategorieDto updated = categorieService.updateCategorie(id, categorieDto);
        return ResponseEntity.ok(new AApiResponse<>(200, Collections.singletonList(updated), 0, 1, "Categorie updated", 1));
    }

    @PreAuthorize("@permissionChecker.has('CATEGORIES_UTILISATEURS_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deleteCategorie(@PathVariable Long id) {
        categorieService.deleteCategorie(id);
        return ResponseEntity.ok(new AApiResponse<>(200, null, 0, 0, "Categorie deleted", 0));
    }
}