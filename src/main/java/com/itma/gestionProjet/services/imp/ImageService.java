package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.entities.FileData;
import com.itma.gestionProjet.entities.Image;
import com.itma.gestionProjet.exceptions.ImageNotFoundException;
import com.itma.gestionProjet.repositories.FileDataRepository;
import com.itma.gestionProjet.repositories.ImageRepository;
import com.itma.gestionProjet.repositories.UserRepository;
import com.itma.gestionProjet.services.IImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;


@Service
public class ImageService  implements IImageService {

    // Dossier de stockage des fichiers "dossier" sur le disque. Valeur par défaut = ancien chemin codé en dur
    // (comportement inchangé en production) ; surchargé en local via la variable d'environnement APP_UPLOAD_DIR.
    @Value("${app.upload.dir:/home/diallomamadou/Documents/ITMA/DATABACKEND/}")
    private String uploadDir;

    private Path uploadFolder() throws IOException {
        Path folder = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(folder);
        return folder;
    }

    /** Résout un nom de fichier dans le dossier d'upload en refusant tout chemin qui en sortirait ("../"). */
    private Path resolveInUploadFolder(String fileName) throws IOException {
        Path folder = uploadFolder();
        Path target = folder.resolve(fileName).normalize();
        if (!target.startsWith(folder) || target.equals(folder)) {
            throw new IOException("Nom de fichier invalide : " + fileName);
        }
        return target;
    }


    @Autowired
    ImageRepository imageRepository;


    @Autowired
    private FileDataRepository fileDataRepository;


    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Override
    public Image uplaodImage(MultipartFile file) throws IOException {
        /*
         * Ce code commenté est équivalent au code utilisant le design pattern Builder
         * Image image = new Image(null, file.getOriginalFilename(),
         * file.getContentType(), file.getBytes(), null);
         *   return imageRepository.save(image);
         */
        return imageRepository.save(Image.builder().name(file.getOriginalFilename()).type(file.getContentType())
                .image(file.getBytes()).build());
    }

    @Override
    public Image updateImage(Long imageId, MultipartFile file) throws IOException {
        // Vérifier si l'image existe
        Image currentImage = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image not found with id " + imageId));

        // Mettre à jour les propriétés de l'image existante avec les nouvelles données
        currentImage.setName(file.getOriginalFilename());
        currentImage.setType(file.getContentType());
        currentImage.setImage(file.getBytes());

        // Enregistrer et retourner l'image mise à jour
        return imageRepository.save(currentImage);
    }


    @Override
    public Image getImageDetails(Long id) throws IOException {
        return null;
    }

    @Override
    public ResponseEntity<byte[]> getImage(Long id) throws IOException {
        return null;
    }

    @Override
    public void deleteImage(Long id) {

    }

    @Override
    public Image uplaodImageProd(MultipartFile file, Long idProd) throws IOException {
        return null;
    }

    @Override
    public List<Image> getImagesParProd(Long prodId) {
        return null;
    }

    @Override

    public String uploadImageToFileSystem(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        Path filePath = resolveInUploadFolder(fileName);
/*
        FileData fileData=fileDataRepository.save(FileData.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .filePath(filePath).build());
*/
        file.transferTo(filePath);

            return  fileName;

    }
    /*
    public String uploadImageToFileSystem(MultipartFile file) {
        try {
            // Vérifie si le fichier est vide
            if (file.isEmpty()) {
                throw new IOException("Le fichier est vide");
            }

            String filePath = FOLDER_PATH + file.getOriginalFilename();

            // Crée le dossier si nécessaire
            File folder = new File(FOLDER_PATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File dest = new File(filePath);
            file.transferTo(dest);

            if (!dest.exists()) {
                throw new IOException("Le fichier n'a pas pu être sauvegardé");
            }


            return  filePath;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du fichier : " + e.getMessage(), e);
        }
    }

*/

    /*

    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        File file = new File(fileName);

        if (!file.exists()) {
            throw new IOException("Fichier non trouvé : " + fileName);
        }

        return Files.readAllBytes(file.toPath());
    }

     */

/*
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        Optional<FileData> fileData = fileDataRepository.findByName(fileName);
        String filePath=fileData.get().getFilePath();
        byte[] images = Files.readAllBytes(new File(filePath).toPath());
        return images;
    }
*/
    public byte[] downloadImageFromFileSystem(String fileName) throws IOException {
        byte[] images = Files.readAllBytes(resolveInUploadFolder(fileName));
        return images;
    }

}

