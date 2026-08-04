package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.DocumentDTO;
import com.itma.gestionProjet.requests.DocumentRequest;
import com.itma.gestionProjet.services.imp.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PreAuthorize("@permissionChecker.has(#documentRequest.projectId, 'DOCUMENTS_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<DocumentDTO>> createDocument(@RequestBody DocumentRequest documentRequest) {
        AApiResponse<DocumentDTO> response = new AApiResponse<>();
        try {
            DocumentDTO documentDTO = documentService.createDocument(documentRequest);
            response.setResponseCode(HttpStatus.CREATED.value());
            response.setData(List.of(documentDTO));
            response.setMessage("Document created successfully");
            response.setLength(1);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error creating Document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@permissionChecker.has(#documentRequest.projectId, 'DOCUMENTS_MODIFIER')")
    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<DocumentDTO>> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentRequest documentRequest) {
        AApiResponse<DocumentDTO> response = new AApiResponse<>();
        try {
            DocumentDTO documentDTO = documentService.updateDocument(id, documentRequest);
            response.setResponseCode(HttpStatus.OK.value());
            response.setData(List.of(documentDTO));
            response.setMessage("Document updated successfully");
            response.setLength(1);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            response.setResponseCode(HttpStatus.NOT_FOUND.value());
            response.setMessage("Document not found: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error updating Document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> deleteDocument(@PathVariable Long id, @RequestParam Long projectId) {
        AApiResponse<Void> response = new AApiResponse<>();
        try {
            documentService.deleteDocument(id);
            response.setResponseCode(HttpStatus.NO_CONTENT.value());
            response.setMessage("Document deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            response.setResponseCode(HttpStatus.NOT_FOUND.value());
            response.setMessage("Document not found: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error deleting Document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<AApiResponse<DocumentDTO>> getDocumentById(@PathVariable Long id, @RequestParam Long projectId) {
        AApiResponse<DocumentDTO> response = new AApiResponse<>();
        try {
            DocumentDTO documentDTO = documentService.getDocumentById(id);
            response.setResponseCode(HttpStatus.OK.value());
            response.setData(List.of(documentDTO));
            response.setMessage("Document retrieved successfully");
            response.setLength(1);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            response.setResponseCode(HttpStatus.NOT_FOUND.value());
            response.setMessage("Document not found: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error retrieving Document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
    @GetMapping
    public ResponseEntity<AApiResponse<DocumentDTO>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AApiResponse<DocumentDTO> response = new AApiResponse<>();
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentDTO> documentsPage = documentService.getAllDocuments(pageable);
            response.setResponseCode(HttpStatus.OK.value());
            response.setData(documentsPage.getContent());
            response.setOffset(page);
            response.setMax(size);
            response.setMessage("Documents retrieved successfully");
            response.setLength(documentsPage.getTotalElements());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error retrieving Documents: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

     */

    @PreAuthorize("@permissionChecker.has(#projectId, 'DOCUMENTS_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<DocumentDTO>> getAllDocuments(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AApiResponse<DocumentDTO> response = new AApiResponse<>();
        try {
            Pageable pageable = PageRequest.of(page, size);

            // Déclarer la variable documentsPage en dehors des blocs conditionnels
            Page<DocumentDTO> documentsPage;

            if (projectId != null) {
                documentsPage = documentService.getDocumentsByProjectId(projectId, pageable);
            } else {
                documentsPage = documentService.getAllDocuments(pageable);
            }
            response.setResponseCode(HttpStatus.OK.value());
            response.setData(documentsPage.getContent());
            response.setOffset(page);
            response.setMax(size);
            response.setMessage("Documents retrieved successfully");
            response.setLength(documentsPage.getTotalElements());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Error retrieving Documents: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



}
