package com.storage.bytevault.service;

import com.storage.bytevault.entity.AppEntity;
import com.storage.bytevault.entity.ChunkMetaData;
import com.storage.bytevault.repository.AppRepository;
import com.storage.bytevault.repository.ChunkMetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppService {
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    @Autowired
    private AppRepository repository;

    @Autowired
    private ChunkMetaDataRepository chunkRepository;

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

            // 1. Create and save the main file entity first to get its ID
            AppEntity fileEntity = new AppEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setSize(file.getSize());
            fileEntity = repository.save(fileEntity);

            int chunkNumber = 0;
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    String chunkPath = uploadDir + fileName + ".chunk" + chunkNumber;
                    Files.write(Paths.get(chunkPath), Arrays.copyOf(buffer, bytesRead));

                    // 2. Link each chunk to the saved file entity
                    ChunkMetaData metadata = new ChunkMetaData();
                    metadata.setFileId(fileEntity.getId());
                    metadata.setChunkNumber(chunkNumber);
                    metadata.setFilePath(chunkPath);
                    chunkRepository.save(metadata);

                    chunkNumber++;
                }
            }

            fileEntity.setTotalChunks(chunkNumber);
            return repository.save(fileEntity);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    public Optional<AppEntity> getFileById(String id) {
        return repository.findById(id);
    }
}
