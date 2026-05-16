package com.storage.bytevault.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class AppEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String fileName;
    private Long size;
    private String filePath;
}