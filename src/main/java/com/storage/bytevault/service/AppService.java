package com.storage.bytevault.service;

import com.storage.bytevault.entity.AppEntity;
import com.storage.bytevault.entity.ChunkMetaData;
import com.storage.bytevault.entity.ReplicaChunkMetadata;
import com.storage.bytevault.repository.AppRepository;
import com.storage.bytevault.repository.ChunkMetaDataRepository;
import com.storage.bytevault.repository.ReplicaChunkMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AppService {
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    private static final String[] STORAGE_NODES = {"storage/node1/","storage/node2/","storage/node3/","storage/node4/"};

    @Autowired
    private AppRepository repository;

    @Autowired
    private ChunkMetaDataRepository chunkRepository;

    @Autowired
    private ReplicaChunkMetadataRepository replicaRepository;

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
                MessageDigest md = MessageDigest.getInstance("SHA-256");

                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    byte[] actualBytes = Arrays.copyOf(buffer, bytesRead);
                    md.reset();
                    md.update(actualBytes);
                    byte[] digest = md.digest();
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : digest) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }
                    String checksum = hexString.toString();

                    ChunkMetaData metadata = new ChunkMetaData();
                    metadata.setFileId(fileEntity.getId());
                    metadata.setChunkNumber(chunkNumber);
                    metadata.setChecksum(checksum);
                    metadata = chunkRepository.save(metadata);

                    // Replicate to 2 nodes
                    int[] replicaNodes = {chunkNumber % STORAGE_NODES.length, (chunkNumber + 1) % STORAGE_NODES.length};
                    for (int nodeIndex : replicaNodes) {
                        String nodeName = STORAGE_NODES[nodeIndex];
                        String chunkPath = nodeName + fileName + ".chunk" + chunkNumber;
                        Files.write(Paths.get(chunkPath), actualBytes);
                        
                        ReplicaChunkMetadata replica = new ReplicaChunkMetadata();
                        replica.setChunkId(metadata.getId());
                        replica.setNodeName(nodeName);
                        replicaRepository.save(replica);
                        System.out.println("Chunk " + chunkNumber + " saved to " + chunkPath);
                    }
                    chunkNumber++;
                }
            }

            fileEntity.setTotalChunks(chunkNumber);
            return repository.save(fileEntity);

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    public void deleteFile(String id) {
        // 1. Find and delete physical chunk files
        List<ChunkMetaData> chunks = chunkRepository.findByFileIdOrderByChunkNumberAsc(id);
        Optional<AppEntity> fileEntityOpt = repository.findById(id);
        if (fileEntityOpt.isPresent()) {
            String fileName = fileEntityOpt.get().getFileName();
            for (ChunkMetaData chunk : chunks) {
                List<ReplicaChunkMetadata> replicas = replicaRepository.findByChunkId(chunk.getId());
                for (ReplicaChunkMetadata replica : replicas) {
                    String chunkPath = replica.getNodeName() + fileName + ".chunk" + chunk.getChunkNumber();
                    try {
                        Files.deleteIfExists(Paths.get(chunkPath));
                    } catch (IOException e) {
                        System.err.println("Failed to delete chunk file: " + chunkPath);
                    }
                }
                replicaRepository.deleteByChunkId(chunk.getId());
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

        Optional<AppEntity> fileEntityOpt = repository.findById(id);
        if (fileEntityOpt.isEmpty()) {
            throw new RuntimeException("File entity not found");
        }
        String fileName = fileEntityOpt.get().getFileName();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (ChunkMetaData chunk : chunks) {
                List<ReplicaChunkMetadata> replicas = replicaRepository.findByChunkId(chunk.getId());
                boolean chunkReadSuccessfully = false;

                for (ReplicaChunkMetadata replica : replicas) {
                    String chunkPath = replica.getNodeName() + fileName + ".chunk" + chunk.getChunkNumber();
                    Path path = Paths.get(chunkPath);
                    if (Files.exists(path)) {
                        byte[] fileBytes = Files.readAllBytes(path);
                        md.reset();
                        md.update(fileBytes);
                        byte[] digest = md.digest();
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : digest) {
                            String hex = Integer.toHexString(0xff & b);
                            if (hex.length() == 1) hexString.append('0');
                            hexString.append(hex);
                        }
                        String calculatedChecksum = hexString.toString();
                        
                        if (calculatedChecksum.equals(chunk.getChecksum())) {
                            out.write(fileBytes);
                            chunkReadSuccessfully = true;
                            break; // Successfully read from this replica
                        } else {
                            System.err.println("Checksum mismatch for chunk " + chunkPath);
                        }
                    }
                }

                if (!chunkReadSuccessfully) {
                    throw new RuntimeException("Could not read chunk " + chunk.getChunkNumber() + " from any replica");
                }
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Error during file reassembly", e);
        }
    }

    public Optional<AppEntity> getFileById(String id) {
        return repository.findById(id);
    }
}
