package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.services.imp.PapFicheService;
import com.itma.gestionProjet.services.imp.PapFicheService.Categorie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Fiche PAP (écran pap/detail), toutes catégories confondues.
 *
 *   GET /paps/{categorie}/{id}?projectId=              → la PAP
 *   PUT /paps/{categorie}/{id}/dedommager?projectId=   → statutPap = indemnisation_terminee
 *   PUT /paps/{categorie}/{id}/photo?projectId=        → photoPap = nom du fichier envoyé (corps texte)
 *
 * {categorie} : habitat, agricole, place-affaire, economique (ou le code PAPHABITAT…).
 */
@RestController
@RequestMapping("/paps")
public class PapFicheController {

    private final PapFicheService service;

    public PapFicheController(PapFicheService service) {
        this.service = service;
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{categorie}/{id}")
    public ResponseEntity<AApiResponse<Map<String, Object>>> lire(@PathVariable String categorie,
                                                                  @PathVariable Long id,
                                                                  @RequestParam Long projectId) {
        Map<String, Object> pap = service.lire(Categorie.depuis(categorie), id, projectId);
        return ResponseEntity.ok(reponse(pap, "PAP récupérée"));
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{categorie}/{id}/dedommager")
    public ResponseEntity<AApiResponse<Map<String, Object>>> dedommager(@PathVariable String categorie,
                                                                        @PathVariable Long id,
                                                                        @RequestParam Long projectId) {
        Map<String, Object> pap = service.dedommager(Categorie.depuis(categorie), id, projectId);
        return ResponseEntity.ok(reponse(pap, "PAP marquée comme dédommagée"));
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{categorie}/{id}/photo")
    public ResponseEntity<AApiResponse<Map<String, Object>>> photo(@PathVariable String categorie,
                                                                   @PathVariable Long id,
                                                                   @RequestParam Long projectId,
                                                                   @RequestBody String nomFichier) {
        Map<String, Object> pap = service.changerPhoto(Categorie.depuis(categorie), id, projectId, nomFichier);
        return ResponseEntity.ok(reponse(pap, "Photo enregistrée"));
    }

    private static AApiResponse<Map<String, Object>> reponse(Map<String, Object> pap, String message) {
        return new AApiResponse<>(200, List.of(pap), 0, 1, message, 1);
    }
}
