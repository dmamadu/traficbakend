package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.CategoriePartieInteresse;
import com.itma.gestionProjet.services.CategoriePartieInteresseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Pas de module dédié dans GESTION_PERMISSIONS_DOC.md §4 : rattaché à PIP (données de
// classification des parties intéressées), vérifié sur le projet actif comme le reste du module.
@RestController
@RequestMapping("/categoriesPip")
public class CategoriePartieInteresseController {

    @Autowired
    private CategoriePartieInteresseService service;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PIP_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<CategoriePartieInteresse>> getCategories(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max,
            @RequestParam Long projectId) {

        PageRequest pageRequest = PageRequest.of(offset, max);
        Page<CategoriePartieInteresse> categoriesPage = service.getCategories(pageRequest);

        AApiResponse<CategoriePartieInteresse> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setData(categoriesPage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(categoriesPage.getTotalElements());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PIP_VOIR')")
    @GetMapping("/{id}")
    public CategoriePartieInteresse getCategorieById(@PathVariable Long id, @RequestParam Long projectId) {
        return service.getCategorieById(id);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PIP_CREER')")
    @PostMapping
    public CategoriePartieInteresse createCategorie(@RequestBody CategoriePartieInteresse categorie, @RequestParam Long projectId) {
        return service.createCategorie(categorie);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PIP_MODIFIER')")
    @PutMapping("/{id}")
    public CategoriePartieInteresse updateCategorie(@PathVariable Long id, @RequestBody CategoriePartieInteresse categorie, @RequestParam Long projectId) {
        return service.updateCategorie(id, categorie);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PIP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public void deleteCategorie(@PathVariable Long id, @RequestParam Long projectId) {
        service.deleteCategorie(id);
    }
}