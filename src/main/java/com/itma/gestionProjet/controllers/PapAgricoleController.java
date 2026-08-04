package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.DatabasePapAgricoleRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapAgricoleResponseDTO;
import com.itma.gestionProjet.dtos.DatabasePapPlaceAffaireResponseDTO;
import com.itma.gestionProjet.entities.DatabasePapAgricole;
import com.itma.gestionProjet.services.DatabasePapAgricoleService;
import com.itma.gestionProjet.services.imp.ExcelImportDatabasePapAgricoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/papAgricole")
public class PapAgricoleController {

    @Autowired
    private ExcelImportDatabasePapAgricoleService excelImportService;

    @Autowired
    private DatabasePapAgricoleService databasePapAgricoleService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_IMPORTER')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file, @RequestParam Long projectId) {
        try {
            ByteArrayInputStream errorFile = excelImportService.importExcel(file);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=errors.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(errorFile.readAllBytes());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /*
    @GetMapping
    public AApiResponse<DatabasePapAgricoleResponseDTO> getAll(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100000000") int max) {
        try {
            // Appel au service pour récupérer les données
            List<DatabasePapAgricoleResponseDTO> data = databasePapAgricoleService.getAllDatabasePapAgricole(offset, max);

            // Création de la réponse AApiResponse
            AApiResponse<DatabasePapAgricoleResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(data);
            response.setOffset(offset);
            response.setLength(databasePapAgricoleService.getTotalCount());
            response.setMax(max);
            response.setMessage("Successfully retrieved data.");

            return response;
        } catch (Exception e) {
            // Gérer les erreurs et renvoyer un message d'erreur avec le code approprié
            AApiResponse<DatabasePapAgricoleResponseDTO> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error retrieving data: " + e.getMessage());
            return errorResponse;
        }
    }

     */

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public AApiResponse<DatabasePapAgricoleResponseDTO> getAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100000000") int max) {
        try {
            List<DatabasePapAgricoleResponseDTO> data;
            long totalCount;
            if (projectId != null) {
                data = databasePapAgricoleService.getDatabasePapAgricoleByProjectId(projectId, offset, max);
                totalCount = databasePapAgricoleService.getTotalCountByProjectId(projectId);
            } else {
                data = databasePapAgricoleService.getAllDatabasePapAgricole(offset, max);
                totalCount = databasePapAgricoleService.getTotalCount();
            }
            AApiResponse<DatabasePapAgricoleResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(data);
            response.setOffset(offset);
            response.setLength(totalCount);
            response.setMax(max);
            response.setMessage("Successfully retrieved data.");
            return response;
        } catch (Exception e) {
            AApiResponse<DatabasePapAgricoleResponseDTO> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error retrieving data: " + e.getMessage());
            return errorResponse;
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/vulnerability-stats")
    public ResponseEntity<Map<String, Object>> getVulnerabilityStats(
            @RequestParam(required = false) Long projectId) {

        Map<String, Object> stats = databasePapAgricoleService.getVulnerabilityStats(projectId);
        return ResponseEntity.ok(stats);
    }


    // Méthode GET pour récupérer une entité par ID
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public AApiResponse<DatabasePapAgricoleResponseDTO> getById(@PathVariable Long id, @RequestParam Long projectId) {
        try {
            // Appel au service pour récupérer les données par ID
            DatabasePapAgricole data = databasePapAgricoleService.getDatabasePapAgricoleById(id);

            // Création de la réponse AApiResponse
            AApiResponse<DatabasePapAgricoleResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of());
            response.setMessage("Successfully retrieved data.");
            response.setLength(1);

            return response;
        } catch (Exception e) {
            // Gérer l'erreur et renvoyer un message d'erreur
            AApiResponse<DatabasePapAgricoleResponseDTO> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(404);
            errorResponse.setMessage("Entity not found: " + e.getMessage());
            return errorResponse;
        }
    }

    // Méthode POST pour créer de nouvelles entités
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<String>> create(@RequestBody List<DatabasePapAgricoleRequestDTO> requestDTOs, @RequestParam Long projectId) {
        try {
            databasePapAgricoleService.createDatabasePapAgricole(requestDTOs);
            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(List.of("Les entités ont été créées avec succès."));
            response.setMessage("La création des entités a été réalisée avec succès.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.setMessage("Une erreur est survenue lors de la création des entités : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    // Méthode PUT pour mettre à jour une entité
    @PreAuthorize("@permissionChecker.has(#requestDTO.projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<String>> update(@PathVariable Long id, @RequestBody DatabasePapAgricoleRequestDTO requestDTO) {
        try {
            // Appel au service pour mettre à jour l'entité
            databasePapAgricoleService.updateDatabasePapAgricole(id, requestDTO);

            // Création de la réponse AApiResponse pour succès
            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of("Entity updated successfully."));
            response.setMessage("Successfully updated entity.");

            return ResponseEntity.ok(response); // Code HTTP 200 OK
        } catch (Exception e) {
            // Gérer les erreurs et renvoyer un message d'erreur avec le bon code HTTP
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error updating entity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); // Code HTTP 500
        }
    }


    // Méthode DELETE pour supprimer une entité
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public AApiResponse<String> delete(@PathVariable Long id, @RequestParam Long projectId) {
        try {
            databasePapAgricoleService.deleteDatabasePapAgricole(id);

            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of("Entity deleted successfully."));
            response.setMessage("Successfully deleted entity.");

            return response;
        } catch (Exception e) {
            // Gérer les erreurs et renvoyer un message d'erreur
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error deleting entity: " + e.getMessage());
            return errorResponse;
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/byCodePap/{codePap}")
    public ResponseEntity<AApiResponse<DatabasePapAgricole>> getByCodePap(@PathVariable String codePap, @RequestParam Long projectId) {
        DatabasePapAgricole papAgricole = databasePapAgricoleService.getByCodePap(codePap);
        AApiResponse<DatabasePapAgricole> response = new AApiResponse<>(
                200,
                List.of(papAgricole),
                0,
                1,
                "Success",
                1L
        );
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/search")
    public AApiResponse<DatabasePapAgricoleResponseDTO> searchGlobal(
            @RequestParam String searchTerm,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int max) {
        try {
            List<DatabasePapAgricoleResponseDTO> data = databasePapAgricoleService.searchGlobalDatabasePapAgricole(searchTerm, Optional.ofNullable(projectId),offset, max);
            long totalCount = databasePapAgricoleService.getTotalCountForSearch(searchTerm, Optional.ofNullable(projectId));

            AApiResponse<DatabasePapAgricoleResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(data);
            response.setOffset(offset);
            response.setLength(totalCount);
            response.setMax(max);
            response.setMessage("Successfully retrieved data.");
            return response;
        } catch (Exception e) {
            AApiResponse<DatabasePapAgricoleResponseDTO> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error retrieving data: " + e.getMessage());
            return errorResponse;
        }
    }



    // Vider tous les PAPs agricoles d'un projet
    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/project/{projectId}")
    public ResponseEntity<AApiResponse<String>> deleteAllByProjectId(@PathVariable Long projectId) {
        try {
            long countBefore = databasePapAgricoleService.getTotalCountByProjectId(projectId);

            if (countBefore == 0) {
                AApiResponse<String> response = new AApiResponse<>();
                response.setResponseCode(200);
                response.setData(List.of("No agricultural PAPs found for this project."));
                response.setMessage("No data to delete.");
                return ResponseEntity.ok(response);
            }

            databasePapAgricoleService.deleteAllByProjectId(projectId);

            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of("Deleted " + countBefore + " agricultural PAPs from project ID: " + projectId));
            response.setMessage("Successfully deleted all agricultural PAPs for the project.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(400);
            errorResponse.setMessage("Invalid input: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error deleting agricultural PAPs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Supprimer par une liste d'IDs
    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyAuthority('Super Admin', 'Admin')")
    public ResponseEntity<AApiResponse<String>> deleteAllByIds(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                AApiResponse<String> errorResponse = new AApiResponse<>();
                errorResponse.setResponseCode(400);
                errorResponse.setMessage("IDs list cannot be null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            long countBefore = ids.size();
            databasePapAgricoleService.deleteAllByIds(ids);

            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of("Deleted " + countBefore + " agricultural PAPs"));
            response.setMessage("Successfully deleted selected agricultural PAPs.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(400);
            errorResponse.setMessage("Invalid input: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error deleting agricultural PAPs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}

