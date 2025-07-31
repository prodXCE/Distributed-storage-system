package com.minidrive.distributedfiles.controller;

import com.minidrive.distributedfiles.model.ChunkMetadata;
import com.minidrive.distributedfiles.model.FileMetadata;
import com.minidrive.distributedfiles.service.MetadataService;
import com.minidrive.distributedfiles.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;
    private final MetadataService metadataService;

    @Autowired
    public FileController(StorageService storageService, MetadataService metadataService) {
        this.storageService = storageService;
        this.metadataService = metadataService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot upload an empty file.");
        }
        try {
            FileMetadata savedFile = metadataService.storeFile(file);
            return ResponseEntity.ok("File uploaded successfully. File ID: " + savedFile.getId());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during file upload: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Upload process was interrupted.");
        }
    }

    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        FileMetadata fileMetadata = metadataService.findFileById(fileId)
                .orElseThrow(() -> new IOException("File not found with ID: " + fileId));

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileMetadata.getFileName() + "\"");
        response.setContentType(fileMetadata.getMimeType());
        response.setContentLengthLong(fileMetadata.getFileSize());

        for (ChunkMetadata chunk : fileMetadata.getChunks()) {
            byte[] chunkData = null;
            boolean chunkRetrieved = false;

            for (String nodeIdentifier : chunk.getStorageNodeIdentifiers()) {
                try {
                    chunkData = storageService.readChunk(chunk.getId(), nodeIdentifier);
                    System.out.println("Successfully retrieved chunk " + chunk.getId() + " from " + nodeIdentifier);
                    chunkRetrieved = true;
                    break;
                } catch (IOException e) {
                    System.err.println("FAILED to retrieve chunk " + chunk.getId() + " from " + nodeIdentifier
                            + ". Trying next replica...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted while fetching chun " + chunk.getId(), e);
                }
            }

            if (!chunkRetrieved) {
                throw new IOException("Could not retrieve chunk " + chunk.getId()
                        + " from any of its replicas. File is unavailable.");
            }

            response.getOutputStream().write(chunkData);
        }
        response.getOutputStream().flush();
    }
}
