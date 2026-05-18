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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppService {
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    private static final String[] STORAGE_NODES = {"storage/node1/","storage/node2/","storage/node3/","storage/node4/"};

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
            for(String node : STORAGE_NODES) {
                Files.createDirectories(Paths.get(node));
            }
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            AppEntity fileEntity = new AppEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setSize(file.getSize());
            fileEntity = repository.save(fileEntity);

            int chunkNumber = 0;
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    String chunkPath = STORAGE_NODES[chunkNumber % STORAGE_NODES.length] + fileName + ".chunk" + chunkNumber;
                    Files.write(Paths.get(chunkPath), Arrays.copyOf(buffer, bytesRead));
                    System.out.println("Chunk " + chunkNumber + " saved to " + chunkPath);
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

    public void deleteFile(String id) {
        // 1. Find and delete physical chunk files
        List<ChunkMetaData> chunks = chunkRepository.findByFileIdOrderByChunkNumberAsc(id);
        for (ChunkMetaData chunk : chunks) {
            try {
                Files.deleteIfExists(Paths.get(chunk.getFilePath()));
            } catch (IOException e) {
                System.err.println("Failed to delete chunk file: " + chunk.getFilePath());
            }
        }
        
        // 2. Delete chunk metadata from DB
        chunkRepository.deleteAll(chunks);
        
        // 3. Delete main file entity from DB
        repository.deleteById(id);
    }

    public void reassembleAndStream(String id, OutputStream out) {
        List<ChunkMetaData> chunks = chunkRepository.findByFileIdOrderByChunkNumberAsc(id);
        if (chunks.isEmpty()) {
            throw new RuntimeException("File chunks not found");
        }

        for (ChunkMetaData chunk : chunks) {
            try {
                Files.copy(Paths.get(chunk.getFilePath()), out);
            } catch (IOException e) {
                throw new RuntimeException("Error during file reassembly", e);
            }
        }
    }

    public Optional<AppEntity> getFileById(String id) {
        return repository.findById(id);
    }
}
