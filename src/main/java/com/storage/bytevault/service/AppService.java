package com.storage.bytevault.service;

import com.storage.bytevault.entity.AppEntity;
import com.storage.bytevault.repository.AppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppService {

    @Autowired
    private AppRepository repository;

    public List<AppEntity> getFiles() {
        return repository.findAll();
    }

    public AppEntity saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        try {
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            String finalPath = uploadDir + fileName;
            Files.copy(file.getInputStream(), Paths.get(finalPath));
            AppEntity entity = new AppEntity();
            entity.setFileName(fileName);
            entity.setSize(file.getSize());
            entity.setFilePath(finalPath);
            return repository.save(entity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    public Optional<AppEntity> getFileById(String id) {
        return repository.findById(id);
    }
}
