package com.storage.bytevault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.storage.bytevault.entity.*;

@Repository
public interface AppRepository extends JpaRepository<AppEntity, String> {
}