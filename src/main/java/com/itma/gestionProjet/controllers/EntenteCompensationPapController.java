package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.EntenteCompensationPapDto;
import com.itma.gestionProjet.requests.EntenteCompensationPapRequest;
import com.itma.gestionProjet.services.EntenteCompensationPapService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/ententes")
public class EntenteCompensationPapController {

    @Autowired
    private EntenteCompensationPapService ententeService;

    /*
    @GetMapping
    public ResponseEntity<Page<EntenteCompensationPapDto>> getAllEntentes(
            @RequestParam Long projectId,
            Pageable pageable) {
        Page<EntenteCompensationPapDto> ententes = ententeService.getAllEntentes(pageable, projectId);
        return ResponseEntity.ok(ententes);
    }
     */
    @PreAuthorize("@permissionChecker.has(#projectId, 'ENTENTE_COMPENSATION_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<EntenteCompensationPapDto>> getAllEntentes(
            @RequestParam(required = false) Long projectId,
            Pageable pageable) {
        Page<EntenteCompensationPapDto> ententes;
        long totalCount;
        if (projectId != null) {
            ententes = ententeService.getEntentesByProjectId(projectId, pageable);
        } else {
            ententes = ententeService.getAllEntentes(pageable);
        }
        AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setData(ententes.getContent());
        response.setOffset(ententes.getPageable().getPageNumber());
        response.setMax(ententes.getPageable().getPageSize());
        response.setLength(ententes.getTotalElements());
        response.setMessage("Successfully retrieved data.");
        return ResponseEntity.ok(response);
    }




    /*
    @PostMapping
    public ResponseEntity<EntenteCompensationPapDto> createEntente(@RequestBody EntenteCompensationPapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ententeService.createEntente(request));
    }

     */
    @PreAuthorize("@permissionChecker.has(#request.projectId, 'ENTENTE_COMPENSATION_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<EntenteCompensationPapDto>> createEntente(@RequestBody EntenteCompensationPapRequest request) {
        try {
            // Appel au service pour créer l'entité
            EntenteCompensationPapDto createdEntente = ententeService.createEntente(request);
            // Construction de la réponse AApiResponse
            AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>();
            response.setResponseCode(201);  // Code de réussite pour la création
            response.setData(Collections.singletonList(createdEntente));  // Nous mettons l'entité créée dans la liste
            response.setOffset(0);  // Pas de pagination ici, donc on met 0
            response.setMax(1);  // Une seule entité a été créée
            response.setLength(1);  // La longueur est de 1
            response.setMessage("Entente successfully created.");  // Message de succès

            // Retourner la réponse enveloppée dans un ResponseEntity
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Gérer les erreurs
            AApiResponse<EntenteCompensationPapDto> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);  // Code d'erreur
            errorResponse.setMessage("Error creating entente: " + e.getMessage());  // Message d'erreur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /*
    @PutMapping("/{id}")
    public ResponseEntity<EntenteCompensationPapDto> updateEntente(
            @PathVariable Long id,
            @RequestBody EntenteCompensationPapRequest request) {
        return ResponseEntity.ok(ententeService.updateEntente(id, request));
    }

     */
    @PreAuthorize("@permissionChecker.has(#request.projectId, 'ENTENTE_COMPENSATION_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<EntenteCompensationPapDto>> updateEntente(
            @PathVariable Long id,
            @RequestBody EntenteCompensationPapRequest request) {
        try {
            EntenteCompensationPapDto updatedEntente = ententeService.updateEntente(id, request);
            AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>();
            response.setResponseCode(200);  // Code de réussite pour la mise à jour
            response.setData(Collections.singletonList(updatedEntente));  // Nous mettons l'entité mise à jour dans la liste
            response.setOffset(0);  // Pas de pagination ici, donc on met 0
            response.setMax(1);  // Une seule entité a été mise à jour
            response.setLength(1);  // La longueur est de 1
            response.setMessage("Entente successfully updated.");  // Message de succès
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AApiResponse<EntenteCompensationPapDto> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);  // Code d'erreur
            errorResponse.setMessage("Error updating entente: " + e.getMessage());  // Message d'erreur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'ENTENTE_COMPENSATION_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deleteEntente(@PathVariable Long id, @RequestParam Long projectId) {
        try {
            ententeService.deleteEntente(id);
            AApiResponse<Void> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setMessage("Entente successfully deleted.");

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            AApiResponse<Void> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(404);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            AApiResponse<Void> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error deleting entente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'ENTENTE_COMPENSATION_VOIR')")
    @GetMapping("/byCodePap")
    public ResponseEntity<AApiResponse> getEntenteByCodePap(@RequestParam String codePap, @RequestParam Long projectId) {
        try {
            EntenteCompensationPapDto entente = ententeService.getEntenteByCodePap(codePap);
            List<EntenteCompensationPapDto> data = Collections.singletonList(entente);
            AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>(
                    200,
                    data,
                    0,
                    1,
                    "Entente récupérée avec succès",
                    1
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>(
                    404,
                    Collections.emptyList(),
                    0,
                    0,
                    e.getMessage(),
                    0
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'ENTENTE_COMPENSATION_VOIR')")
    @GetMapping("/search")
    public AApiResponse<EntenteCompensationPapDto> searchGlobal(
            @RequestParam String searchTerm,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int max) {
        try {
            Pageable pageable = PageRequest.of(offset, max);
            Page<EntenteCompensationPapDto> resultPage = ententeService.searchGlobalEntenteCompensationPap(searchTerm, Optional.ofNullable(projectId),pageable);
            AApiResponse<EntenteCompensationPapDto> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(resultPage.getContent());
            response.setOffset(offset);
            response.setLength(resultPage.getTotalElements());
            response.setMax(max);
            response.setMessage("Successfully retrieved data.");
            return response;
        } catch (Exception e) {
            AApiResponse<EntenteCompensationPapDto> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error retrieving data: " + e.getMessage());
            return errorResponse;
        }
    }

}

