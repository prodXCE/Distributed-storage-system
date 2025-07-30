package com.minidrive.distributedfiles.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;


@Entity
@Table(name = "chunks")
@Data
public class ChunkMetadata {


    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(nullable = false)
    private int chunkIndex; // the sequential order of this chunk (0, 1, 2, ..)


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chunk_locations", joinColumns = @JoinColumn(name = "chunk_id"))
    @Column(name = "node_identifier", nullable = false)
    private List<String> storageNodeIdentifiers;

}