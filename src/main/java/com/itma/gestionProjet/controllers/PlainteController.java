package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.PlainteDto;
import com.itma.gestionProjet.dtos.PlainteImportResultDto;
import com.itma.gestionProjet.dtos.PlainteInvalidDto;
import com.itma.gestionProjet.requests.PlainteRequest;
import com.itma.gestionProjet.services.PlainteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/plaintes")
public class PlainteController {

    @Autowired
    private PlainteService plainteService;

    @PreAuthorize("@permissionChecker.has(#plainteRequest.projectId, 'PLAINTES_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<PlainteDto>> createPlainte(@RequestBody PlainteRequest plainteRequest) {
        PlainteDto createdPlainte = plainteService.createPlainte(plainteRequest);
        AApiResponse<PlainteDto> response = new AApiResponse<>();
        response.setResponseCode(201);
        response.setMessage("Plainte créée avec succès");
        response.setData(Collections.singletonList(createdPlainte));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_CREER')")
    @PostMapping("/importer")
    public ResponseEntity<AApiResponse<Map<String, Object>>> createPlaintes(@RequestBody List<PlainteRequest> plainteRequests, @RequestParam Long projectId) {
        List<PlainteDto> plaintesValides = new ArrayList<>();
        List<PlainteInvalidDto> plaintesInvalides = new ArrayList<>();

        for (PlainteRequest plainteRequest : plainteRequests) {
            try {
                plaintesValides.add(plainteService.createPlainte(plainteRequest));
            } catch (Exception e) {
                PlainteInvalidDto invalidDto = new PlainteInvalidDto();
                invalidDto.setPlainteRequest(plainteRequest);
                invalidDto.setErrorMessage(e.getMessage());
                plaintesInvalides.add(invalidDto);
            }
        }

        AApiResponse<Map<String, Object>> response = new AApiResponse<>();
        if (plaintesValides.isEmpty() && !plaintesInvalides.isEmpty()) {
            response.setResponseCode(400);
            response.setMessage("Toutes les plaintes sont invalides");
        } else if (plaintesInvalides.isEmpty()) {
            response.setResponseCode(201);
            response.setMessage("Toutes les plaintes ont été créées avec succès");
        } else {
            response.setResponseCode(207);
            response.setMessage("Certaines plaintes sont invalides");
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("plaintesValides", plaintesValides);
        responseData.put("plaintesInvalides", plaintesInvalides);
        response.setData(Collections.singletonList(responseData));
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_CREER')")
    @PostMapping("/import")
    public ResponseEntity<AApiResponse<PlainteImportResultDto>> importerExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) {
        AApiResponse<PlainteImportResultDto> response = new AApiResponse<>();
        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xlsx")) {
            response.setResponseCode(400);
            response.setMessage("Seuls les fichiers .xlsx sont acceptés");
            return ResponseEntity.badRequest().body(response);
        }
        PlainteImportResultDto result = plainteService.importerDepuisExcel(file, projectId);
        response.setResponseCode(200);
        response.setMessage("Import terminé");
        response.setData(Collections.singletonList(result));
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<PlainteDto>> getAllPlaintes(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max) {
        Pageable pageable = PageRequest.of(offset, max);
        Page<PlainteDto> plaintePage = projectId != null
                ? plainteService.getPlaintesByProjectId(projectId, pageable)
                : plainteService.getAllPlaintes(pageable);
        AApiResponse<PlainteDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Liste des plaintes récupérée avec succès");
        response.setData(plaintePage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(plaintePage.getTotalElements());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<AApiResponse<PlainteDto>> getPlainteById(@PathVariable Long id, @RequestParam Long projectId) {
        PlainteDto plainte = plainteService.getPlainteById(id);
        AApiResponse<PlainteDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Plainte récupérée avec succès");
        response.setData(Collections.singletonList(plainte));
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#plainteRequest.projectId, 'PLAINTES_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<PlainteDto>> updatePlainte(@PathVariable Long id, @RequestBody PlainteRequest plainteRequest) {
        PlainteDto updatedPlainte = plainteService.updatePlainte(id, plainteRequest);
        AApiResponse<PlainteDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Plainte mise à jour avec succès");
        response.setData(Collections.singletonList(updatedPlainte));
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deletePlainte(@PathVariable Long id, @RequestParam Long projectId) {
        plainteService.deletePlainte(id);
        AApiResponse<Void> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Plainte supprimée avec succès");
        return ResponseEntity.ok().body(response);
    }

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
            plainteService.deleteAllByIds(ids);

            AApiResponse<String> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(List.of("Deleted " + countBefore + " plaintes"));
            response.setMessage("Plaintes supprimées avec succès.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(400);
            errorResponse.setMessage("Invalid input: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            AApiResponse<String> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error deleting plaintes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_VOIR')")
    @GetMapping("/by-codePap")
    public ResponseEntity<AApiResponse<PlainteDto>> getPlainteByCodePap(
            @RequestParam String codePap,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long projectId) {
        AApiResponse<PlainteDto> response = plainteService.getPlainteByCodePap(codePap, page, size);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PLAINTES_VOIR')")
    @GetMapping("/search")
    public ResponseEntity<AApiResponse<PlainteDto>> searchGlobal(
            @RequestParam String searchTerm,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        AApiResponse<PlainteDto> response = plainteService.searchGlobalPlaintes(
                searchTerm, Optional.ofNullable(projectId), page, size);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }
}