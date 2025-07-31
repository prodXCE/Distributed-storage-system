package com.minidrive.distributedfiles.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    // --- CRITICAL CHANGE: Point to the VM's IP with the forwarded ports ---
    private static final List<String> STORAGE_NODES = List.of(
            "http://192.168.56.10:8081", // This now goes to jail 1
            "http://192.168.56.10:8082", // This now goes to jail 2
            "http://192.168.56.10:8083" // This now goes to jail 3
    );

    private static final int REPLICATION_FACTOR = 2;
    private static final int CHUNK_SIZE = 1024 * 1024 * 5; // 5 MB

    private final HttpClient httpClient;

    public StorageService() {
        if (STORAGE_NODES.size() < REPLICATION_FACTOR) {
            throw new RuntimeException("Replication factor is greater than the number of nodes.");
        }
        // Create a reusable HTTP client.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<ChunkInfo> storeChunks(InputStream inputStream, String fileId)
            throws IOException, InterruptedException {
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
                    String nodeUrl = STORAGE_NODES.get(nodeIndex);

                    // Call the new network method to write the chunk.
                    writeChunkToNode(chunkData, chunkId, nodeUrl);

                    savedLocations.add(nodeUrl);
                }
                chunkInfos.add(new ChunkInfo(chunkId, savedLocations));
                chunkIndex++;
            }
        }
        return chunkInfos;
    }

    // NEW METHOD: Writes a chunk to a storage node over HTTP.
    private void writeChunkToNode(byte[] chunkData, String chunkId, String nodeUrl)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUrl + "/chunks/" + chunkId))
                .POST(HttpRequest.BodyPublishers.ofByteArray(chunkData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) { // 201 Created
            throw new IOException(
                    "Failed to store chunk " + chunkId + " on node " + nodeUrl + ". Status: " + response.statusCode());
        }
    }

    // UPDATED METHOD: Reads a chunk from a storage node over HTTP.
    public byte[] readChunk(String chunkId, String nodeUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUrl + "/chunks/" + chunkId))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) { // 200 OK
            throw new IOException(
                    "Failed to read chunk " + chunkId + " from node " + nodeUrl + ". Status: " + response.statusCode());
        }
        return response.body();
    }
}
