package com.storage.bytevault.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.storage.bytevault.entity.AppEntity;
import com.storage.bytevault.service.AppService;
import java.util.List;

@RestController
@RequestMapping("/")
public class AppController {

    @Autowired
    private AppService service;

    @GetMapping("/files")
    public List<AppEntity> getAllFiles() {
        return service.getFiles();
    }

    @PostMapping("/files/upload")
    public AppEntity uploadFile(@RequestParam("file") MultipartFile file) {
        return service.saveFile(file);
    }

    @GetMapping("/files/{id}")
    public AppEntity getFileById(@PathVariable String id) {
        return service.getFileById(id).orElse(null);
    }
}
