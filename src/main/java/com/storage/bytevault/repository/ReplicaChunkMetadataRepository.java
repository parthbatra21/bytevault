package com.storage.bytevault.repository;

import com.storage.bytevault.entity.ReplicaChunkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplicaChunkMetadataRepository extends JpaRepository<ReplicaChunkMetadata, String> {
    List<ReplicaChunkMetadata> findByChunkId(String chunkId);
    void deleteByChunkId(String chunkId);
}
