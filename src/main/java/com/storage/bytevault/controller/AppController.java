package com.storage.bytevault.controller;

import com.storage.bytevault.entity.AppEntity;
import com.storage.bytevault.service.AppService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/")
public class AppController {

    private final AppService service;

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

    @DeleteMapping("/files/{id}")
    public void deleteFile(@PathVariable String id) {
        service.deleteFile(id);
    }

    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) throws IOException {
        AppEntity fileEntity = service.getFileById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Use Piped streams to reassemble chunks on the fly without loading everything into memory
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        Thread.startVirtualThread(() -> {
            try (out) {
                service.reassembleAndStream(id, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}
