package com.minidrive.distributedfiles.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class StorageService {

    @AllArgsConstructor
    @Getter
    public static class ChunkInfo {
        private final String chunkId;
        private final List<String> locations;
    }

    private static final List<Path> STORAGE_NODES = List.of(
            Paths.get("storage/node1"),
            Paths.get("storage/node2"),
            Paths.get("storage/node3"));

    private static final int REPLICATION_FACTOR = 2;

    private static final int CHUNK_SIZE = 1024 * 1024 * 5; // 5 MB

    public StorageService() {
        if (STORAGE_NODES.size() < REPLICATION_FACTOR) {
            throw new RuntimeException(
                    "Replication factor cannot be greater than the number of available storage nodes.");
        }
        for (Path nodePath : STORAGE_NODES) {
            try {
                Files.createDirectories(nodePath);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage location: " + nodePath, e);
            }
        }
    }

    public List<ChunkInfo> storeChunks(InputStream inputStream, String fileId) throws IOException {
        List<ChunkInfo> chunkInfos = new ArrayList<>();
        int chunkIndex = 0;
        try (inputStream) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunkId = fileId + "_part_" + chunkIndex;
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                List<String> savedLocations = new ArrayList<>();
                for (int i = 0; i < REPLICATION_FACTOR; i++) {
                    int nodeIndex = (chunkIndex + i) % STORAGE_NODES.size();
                    Path nodePath = STORAGE_NODES.get(nodeIndex);
                    Path chunkPath = nodePath.resolve(chunkId);

                    Files.write(chunkPath, chunkData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    savedLocations.add(nodePath.toString());
                }

                chunkInfos.add(new ChunkInfo(chunkId, savedLocations));
                chunkIndex++;
            }
        }
        return chunkInfos;
    }

    public byte[] readChunk(String chunkId, String nodeIdentifier) throws IOException {
        Path chunkPath = Paths.get(nodeIdentifier).resolve(chunkId);
        if (!Files.exists(chunkPath)) {
            throw new IOException("Chunk not found: " + chunkId + " at node " + nodeIdentifier);
        }
        return Files.readAllBytes(chunkPath);
    }
}
