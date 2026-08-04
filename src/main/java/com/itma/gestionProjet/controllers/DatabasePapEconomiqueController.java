package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.DatabasePapEconomiqueDto;
import com.itma.gestionProjet.entities.DatabasePapEconomique;
import com.itma.gestionProjet.requests.DatabasePapEconomiqueRequest;
import com.itma.gestionProjet.services.DatabasePapEconomiqueService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/papEconomique")
public class DatabasePapEconomiqueController {

    private final DatabasePapEconomiqueService service;

    public DatabasePapEconomiqueController(DatabasePapEconomiqueService service) {
        this.service = service;
    }
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<DatabasePapEconomiqueDto>> createPapEconomiques(@RequestBody List<DatabasePapEconomiqueRequest> requests, @RequestParam Long projectId) {
        try {
            List<DatabasePapEconomiqueDto> dtos = service.createPapEconomiques(requests);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AApiResponse<>(
                            HttpStatus.CREATED.value(),
                            dtos,
                            0, // Offset non applicable ici
                            dtos.size(),
                            "PAP économiques créés avec succès",
                            dtos.size()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AApiResponse<>(
                            HttpStatus.BAD_REQUEST.value(),
                            null,
                            0,
                            0,
                            "Erreur lors de la création des PAP économiques" +e,
                            0
                    ));
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<DatabasePapEconomiqueDto>> getAllPapEconomiques(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100000000") int max,
            @RequestParam(required = false) Long projectId) {
        try {
            Page<DatabasePapEconomiqueDto> page = projectId != null
                    ? service.getPapEconomiquesByProjectId(projectId, offset, max)
                    : service.getAllPapEconomiques(offset, max);
            return ResponseEntity.ok(new AApiResponse<>(
                    HttpStatus.OK.value(),
                    page.getContent(),
                    offset,
                    max,
                    "Liste des PAP économiques récupérée avec succès",
                    page.getTotalElements()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AApiResponse<>(
                            HttpStatus.BAD_REQUEST.value(),
                            null,
                            offset,
                            max,
                            "Erreur lors de la récupération des PAP économiques",
                            0
                    ));
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<AApiResponse<DatabasePapEconomiqueDto>> getPapEconomiqueById(@PathVariable Long id, @RequestParam Long projectId) {
        try {
            DatabasePapEconomiqueDto dto = service.getPapEconomiqueById(id);
            return ResponseEntity.ok(new AApiResponse<>(
                    HttpStatus.OK.value(),
                    List.of(dto),
                    0,
                    1,
                    "PAP économique récupéré avec succès",
                    1
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AApiResponse<>(
                            HttpStatus.NOT_FOUND.value(),
                            null,
                            0,
                            0,
                            "PAP économique non trouvé",
                            0
                    ));
        }
    }

    @PreAuthorize("@permissionChecker.has(#request.projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<DatabasePapEconomiqueDto>> updatePapEconomique(
            @PathVariable Long id, @RequestBody DatabasePapEconomiqueRequest request) {
        try {
            DatabasePapEconomiqueDto dto = service.updatePapEconomique(id, request);
            return ResponseEntity.ok(new AApiResponse<>(
                    HttpStatus.OK.value(),
                    List.of(dto),
                    0,
                    1,
                    "PAP économique mis à jour avec succès",
                    1
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AApiResponse<>(
                            HttpStatus.BAD_REQUEST.value(),
                            null,
                            0,
                            0,
                            "Erreur lors de la mise à jour du PAP économique"+e,
                            0
                    ));
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deletePapEconomique(@PathVariable Long id, @RequestParam Long projectId) {
        try {
            service.deletePapEconomique(id);
            return ResponseEntity.ok(new AApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    0,
                    0,
                    "PAP économique supprimé avec succès",
                    0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AApiResponse<>(
                            HttpStatus.NOT_FOUND.value(),
                            null,
                            0,
                            0,
                            "Erreur lors de la suppression du PAP économique",
                            0
                    ));
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/byCodePap/{codePap}")
    public ResponseEntity<AApiResponse<DatabasePapEconomique>> getByCodePap(@PathVariable String codePap, @RequestParam Long projectId) {
        DatabasePapEconomique papEconomique = service.getByCodePap(codePap);
        AApiResponse<DatabasePapEconomique> response = new AApiResponse<>(
                200, // responseCode
                List.of(papEconomique), // data
                0, // offset
                1, // max
                "Success", // message
                1L // length
        );
        return ResponseEntity.ok(response);
    }
}
