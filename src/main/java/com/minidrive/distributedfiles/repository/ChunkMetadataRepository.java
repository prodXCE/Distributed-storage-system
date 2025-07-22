package com.minidrive.distributedfiles.repository;

import com.minidrive.distributedfiles.model.ChunkMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChunkMetadata entity.
 */
@Repository
public interface ChunkMetadataRepository extends JpaRepository<ChunkMetadata, String> {
}
