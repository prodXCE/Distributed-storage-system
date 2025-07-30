package com.minidrive.distributedfiles.repository;

import com.minidrive.distributedfiles.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

}
