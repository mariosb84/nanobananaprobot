package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.dto.HiggsfieldImageRequest;
import org.example.nanobananaprobot.domain.dto.HiggsfieldStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Slf4j
@Service
public class HiggsfieldImageService {

    @Value("${higgsfield.client-id}")
    private String clientId;

    @Value("${higgsfield.client-secret}")
    private String clientSecret;

    private static final String BASE_URL = "https://platform.higgsfield.ai";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HiggsfieldImageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateImage(String prompt) throws Exception {

        /* 1. Запускаем генерацию*/

        String requestId = startGeneration(prompt);

        /* 2. Ждём и проверяем статус*/

        return waitForResult(requestId);
    }

    private String startGeneration(String prompt) throws Exception {
        HiggsfieldImageRequest request = new HiggsfieldImageRequest();
        request.setPrompt(prompt);

        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/higgsfield-ai/soul/standard"))
                .header("Authorization", "Key " + clientId + ":" + clientSecret)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String requestId = root.path("request_id").asText();
            log.info("Higgsfield: генерация запущена, request_id: {}", requestId);
            return requestId;
        } else {
            throw new RuntimeException("Ошибка запуска: " + response.body());
        }
    }

    private String waitForResult(String requestId) throws Exception {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(10000);

            String statusUrl = BASE_URL + "/requests/" + requestId + "/status";
            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Key " + clientId + ":" + clientSecret)
                    .GET()
                    .build();

            HttpResponse<String> statusResponse = httpClient.send(statusRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (statusResponse.statusCode() == 200) {
                HiggsfieldStatusResponse status = objectMapper.readValue(
                        statusResponse.body(), HiggsfieldStatusResponse.class);

                if ("completed".equals(status.getStatus())) {
                    if (status.getImages() != null && !status.getImages().isEmpty()) {
                        return status.getImages().get(0).getUrl();
                    }
                } else if ("failed".equals(status.getStatus())) {
                    throw new RuntimeException("Генерация не удалась");
                }

                /* Если still processing - продолжаем ждать*/
            }
        }
        throw new RuntimeException("Таймаут ожидания результата");
    }

}
