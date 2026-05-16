package com.storage.bytevault.repository;

import com.storage.bytevault.entity.ChunkMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkMetaDataRepository extends JpaRepository<ChunkMetaData, String> {
}
