package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.entities.Fichier;
import com.itma.gestionProjet.services.IFichierService;
import com.itma.gestionProjet.services.imp.FichierServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fichiers")
public class FichierController {

    @Autowired
    private FichierServiceImpl fichierService;

    // Ajouter un nouveau fichier
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_CREER')")
    @PostMapping("/ajout/{projectId}")
    public ResponseEntity<Fichier> addFichierToProject(@PathVariable Long projectId, @RequestBody Fichier fichier) {
        Fichier newFichier = fichierService.addFichier(projectId, fichier);
        return ResponseEntity.ok(newFichier);
    }

    // Obtenir un fichier par ID
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<Fichier> getFichierById(@PathVariable Long id, @RequestParam Long projectId) {
        Fichier fichier = fichierService.getFichierById(id);
        return ResponseEntity.ok(fichier);
    }

    // Obtenir tous les fichiers
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_VOIR')")
    @GetMapping
    public ResponseEntity<List<Fichier>> getAllFichiers(@RequestParam Long projectId) {
        List<Fichier> fichiers = fichierService.getAllFichiers();
        return ResponseEntity.ok(fichiers);
    }

    // Mettre à jour un fichier
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<Fichier> updateFichier(@PathVariable Long id, @RequestBody Fichier fichierDetails, @RequestParam Long projectId) {
        Fichier updatedFichier = fichierService.updateFichier(id, fichierDetails);
        return ResponseEntity.ok(updatedFichier);
    }

    // Supprimer un fichier
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFichier(@PathVariable Long id, @RequestParam Long projectId) {
        fichierService.deleteFichier(id);
        return ResponseEntity.noContent().build();
    }

    // Obtenir les fichiers par ID de projet
    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_VOIR')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Fichier>> getFichiersByProjectId(@PathVariable Long projectId) {
        List<Fichier> fichiers = fichierService.getFichiersByProjectId(projectId);
        return ResponseEntity.ok(fichiers);
    }
}