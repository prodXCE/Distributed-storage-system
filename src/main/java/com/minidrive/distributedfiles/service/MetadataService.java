package com.minidrive.distributedfiles.service;

import com.minidrive.distributedfiles.model.ChunkMetadata;
import com.minidrive.distributedfiles.model.FileMetadata;
import com.minidrive.distributedfiles.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MetadataService {

    private final FileMetadataRepository fileMetadataRepository;
    private final StorageService storageService;

    @Autowired
    public MetadataService(FileMetadataRepository fileMetadataRepository, StorageService storageService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.storageService = storageService;
    }

    @Transactional
    public FileMetadata storeFile(MultipartFile file) throws IOException, InterruptedException {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(file.getOriginalFilename());
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setMimeType(file.getContentType());

        FileMetadata savedFileMetadata = fileMetadataRepository.save(fileMetadata);

        List<StorageService.ChunkInfo> chunkInfos = storageService.storeChunks(file.getInputStream(),
                savedFileMetadata.getId());

        List<ChunkMetadata> chunkMetadataList = new ArrayList<>();
        for (int i = 0; i < chunkInfos.size(); i++) {
            StorageService.ChunkInfo info = chunkInfos.get(i);
            ChunkMetadata chunkMetadata = new ChunkMetadata();
            chunkMetadata.setId(info.getChunkId());
            chunkMetadata.setChunkIndex(i);
            chunkMetadata.setStorageNodeIdentifiers(info.getLocations());
            chunkMetadata.setFile(savedFileMetadata); // Link to the managed parent entity.
            chunkMetadataList.add(chunkMetadata);
        }
        savedFileMetadata.setChunks(chunkMetadataList);

        return fileMetadataRepository.save(savedFileMetadata);
    }

    public Optional<FileMetadata> findFileById(String fileId) {
        return fileMetadataRepository.findById(fileId);
    }
}
