package com.itma.gestionProjet.services;


import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final MinioClient minioClient;

    public FileStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Crée le bucket au démarrage s'il n'existe pas (nouvel environnement, MinIO local vierge).
     * Sans lui, tout upload échoue en 500 (NoSuchBucket). Un MinIO injoignable ne bloque pas le
     * démarrage : on journalise et l'erreur réapparaîtra au premier upload.
     */
    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket MinIO '{}' créé.", bucketName);
            }
        } catch (Exception e) {
            log.warn("Impossible de vérifier/créer le bucket MinIO '{}' : {}", bucketName, e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            return fileName;
        } catch (Exception e) {
            log.error("Échec de l'upload MinIO de '{}' : {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload du fichier", e);
        }
    }

    public byte[] getFile(String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());

            return IOUtils.toByteArray(stream);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération du fichier", e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier", e);
        }
    }
}
