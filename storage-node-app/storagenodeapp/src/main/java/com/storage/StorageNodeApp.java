package com.storage;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;

public class StorageNodeApp {

    private static final Path STORAGE_DIRECTORY = Paths.get("/data/chunks");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(STORAGE_DIRECTORY);

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/chunks/", exchange -> {
            try {
                String method = exchange.getRequestMethod();
                String chunkId = Paths.get(exchange.getRequestURI().getPath()).getFileName().toString();
                Path chunkFile = STORAGE_DIRECTORY.resolve(chunkId);

                if ("POST".equalsIgnoreCase(method)) {
                    try (InputStream requestBody = exchange.getRequestBody()) {
                        Files.write(chunkFile, requestBody.readAllBytes(), StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    }
                    exchange.sendResponseHeaders(201, -1); // 201 Created
                } else if ("GET".equalsIgnoreCase(method)) {
                    if (Files.exists(chunkFile)) {
                        // For GET, read the file and write it to the response body.
                        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                        exchange.sendResponseHeaders(200, Files.size(chunkFile)); // 200 OK
                        try (OutputStream responseBody = exchange.getResponseBody()) {
                            Files.copy(chunkFile, responseBody);
                        }
                    } else {
                        exchange.sendResponseHeaders(404, -1); // 404 Not Found
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
                }
            } catch (Exception e) {
                System.err.println("Error processing request: " + e.getMessage());
                exchange.sendResponseHeaders(500, -1); // 500 Internal Server Error
            } finally {
                exchange.close();
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Storage Node server started on port 8081. Storing data in: " + STORAGE_DIRECTORY);
    }
}
