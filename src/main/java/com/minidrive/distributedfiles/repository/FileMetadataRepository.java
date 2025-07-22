package com.minidrive.distributedfiles.repository;

import com.minidrive.distributedfiles.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FileMetadata entity.
 * @Repository - Marks this as a Spring component for data access.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {
    // Spring Data JPA is smart. If we need custom queries later, we can just define
    // a method signature here, e.g., List<FileMetadata> findByFileName(String fileName);
    // and Spring will automatically implement it for us.
}
