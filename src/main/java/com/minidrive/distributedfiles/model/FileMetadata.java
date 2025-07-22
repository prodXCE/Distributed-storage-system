package com.minidrive.distributedfiles.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An Entity representing the metadata for a complete file.
 * This class maps to the "files" table in the database.
 */
@Entity
@Table(name = "files")
@Data
public class FileMetadata {


    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String fileName;

    private long fileSize;

    private String mimeType;

    @Column(nullable = false)
    private Instant uploadTimestamp;


    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("chunkIndex ASC")
    private List<ChunkMetadata> chunks = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        this.id = UUID.randomUUID().toString();
        this.uploadTimestamp = Instant.now();
    }
}
