package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.TacheDTO;
import com.itma.gestionProjet.dtos.TacheResponseDTO;
import com.itma.gestionProjet.entities.Tache;
import com.itma.gestionProjet.services.imp.TacheServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/taches")
public class TacheController {

    @Autowired
    private TacheServiceImpl tacheService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_CREER')")
    @PostMapping

    public ResponseEntity<AApiResponse<Tache>> createTache(@RequestBody Tache tache,@RequestParam Long projectId) {
        AApiResponse<Tache> response = new AApiResponse<>();
        try {
            Tache createdTache = tacheService.createTache(tache,projectId);
            response.setResponseCode(200);
            response.setMessage("Tâche créée avec succès");
            response.setData(Collections.singletonList(createdTache)); // Encapsulez la tâche dans une liste
            response.setOffset(0);
            response.setMax(0);
            response.setLength(1);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<TacheDTO>> getTaches(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max, @RequestParam(required = false) Long projectId) {

        PageRequest pageRequest = PageRequest.of(offset, max);
        Page<TacheDTO> tachesPage;
        if (projectId != null) {
            tachesPage = tacheService.getTachesByProjectId(projectId, pageRequest);
        } else {
            tachesPage = tacheService.getAllTaches(pageRequest);
        }
        AApiResponse<TacheDTO> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setData(tachesPage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(tachesPage.getTotalElements());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<Tache> getTacheById(@PathVariable Long id, @RequestParam Long projectId) {
        Tache tache = tacheService.getTacheById(id);
        return tache != null ? new ResponseEntity<>(tache, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<TacheDTO> updateTache(@PathVariable Long id, @RequestBody Tache tache, @RequestParam Long projectId) {
        TacheDTO updatedTache = tacheService.updateTache(id, tache);
        return new ResponseEntity<>(updatedTache, HttpStatus.OK);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTache(@PathVariable Long id, @RequestParam Long projectId) {
        tacheService.deleteTache(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'TACHES_VOIR')")
    @GetMapping("/consultant/{userId}")
    public ResponseEntity<AApiResponse<TacheResponseDTO>> getTachesByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @RequestParam Long projectId) {

        try {
            Pageable pageable = PageRequest.of(offset, max, Sort.by(Sort.Order.asc("id")));
            Page<Tache> tachePage = tacheService.getTachesByUserId(userId, pageable);

            List<TacheResponseDTO> taches = tachePage.getContent().stream()
                    .map(tacheService::mapToDTO)
                    .collect(Collectors.toList());

            AApiResponse<TacheResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setMessage("Tâches récupérées avec succès");
            response.setData(taches);
            response.setOffset(offset);
            response.setLength(tachePage.getTotalElements());
            response.setMax(max);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            AApiResponse<TacheResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(500);
            response.setMessage("Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
