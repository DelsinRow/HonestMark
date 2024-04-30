package com.delsinrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final HttpClient httpClient;
    private final Semaphore rateLimiter;
    private final ObjectMapper objectMapper;

    /**
     * @param timeUnit - A unit of time that defines a query restriction interval
     * @param requestLimit - The maximum number of requests in the specified time interval
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newBuilder().build();
        this.rateLimiter = new Semaphore(requestLimit);
        this.objectMapper = new ObjectMapper();

        Thread releaser = new Thread(() -> {
            try {
                while (true) {
                    timeUnit.sleep(1);
                    rateLimiter.release(requestLimit - rateLimiter.availablePermits());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        releaser.setDaemon(true);
        releaser.start();
    }

    /**
     * @param document - The document to be sent
     * @param signature - Signature of the document
     * @return - Response from the server
     * @throws InterruptedException - If the stream was interrupted while waiting for resolution
     * @throws IOException - If an I/O error has occurred
     */
    public String createDocument(Object document, String signature) throws InterruptedException, IOException {
        rateLimiter.acquire();

        try {
            String jsonBody = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            rateLimiter.release();
            throw e;
        }
    }

    public static class Document {
        private String participantInn;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private String reg_date;
        private String reg_number;
        private Product[] products;
    }
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
