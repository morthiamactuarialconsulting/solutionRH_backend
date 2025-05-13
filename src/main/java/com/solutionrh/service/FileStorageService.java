package com.solutionrh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String category, String professionalId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Créer le répertoire si nécessaire
        String targetDir = uploadDir + "/" + category + "/" + professionalId;
        Path targetPath = Paths.get(targetDir);
        Files.createDirectories(targetPath);
        
        // Générer un nom de fichier unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        
        // Sauvegarder le fichier
        Path filePath = targetPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Retourner le chemin relatif
        return category + "/" + professionalId + "/" + filename;
    }
    
    public Path getFilePath(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath);
    }
}
