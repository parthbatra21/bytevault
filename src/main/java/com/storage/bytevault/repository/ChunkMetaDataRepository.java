package com.storage.bytevault.repository;

import com.storage.bytevault.entity.ChunkMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkMetaDataRepository extends JpaRepository<ChunkMetaData, String> {
    List<ChunkMetaData> findByFileIdOrderByChunkNumberAsc(String fileId);
}
