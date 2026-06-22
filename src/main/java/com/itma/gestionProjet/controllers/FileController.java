package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.entities.File;
import com.itma.gestionProjet.services.imp.FileService;
import com.itma.gestionProjet.services.imp.ImageService;
import com.itma.gestionProjet.services.imp.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {


    @Autowired
    FileService fileService ;

    @Autowired
    ProjectService projectService;

    @PostMapping(value = "/uploadFilesProd/{idProd}" )
    public File uploadMultiImages(@RequestParam("file") MultipartFile file,
                                  @PathVariable("idProd") Long idProd)
            throws IOException {
        return fileService.uplaodFileProj(file,idProd);
    }



    @PostMapping(value = "/update/{projectId}" )
    public List<File> uploadFiles(@RequestParam("files") MultipartFile[] files, @PathVariable("projectId") Long projectId) {
        try {
            return fileService.updateFiles(files, projectId);
        } catch (Exception e) {
            // Gérer l'exception selon vos besoins
            throw new RuntimeException("Failed to upload files", e);
        }
    }
    /*
    public List<File> getImagesProd(@PathVariable("idProd") Long idProd)
            throws IOException {
        return fileService.getFilesParProj(idProd);
    }


     */

    @RequestMapping(value = "/delete/{projectId}" )
    public List<File> deletefiles(@PathVariable("projectId") Long projectId) {
        try {
            return fileService.deleteFile(projectId);
        } catch (Exception e) {
            // Gérer l'exception selon vos besoins
            throw new RuntimeException("Failed to delete files", e);
        }
    }



}
