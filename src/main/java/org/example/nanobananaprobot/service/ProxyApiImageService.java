package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class ProxyApiImageService {

    @Value("${proxyapi.key}")
    private String apiKey;

    private static final String API_URL = "https://api.proxyapi.ru/openai/v1/images/generations";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProxyApiImageService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateImage(String prompt) throws Exception {              /* models : "dall-e-3"/"sd3-large"/"sd3-medium"/"dall-e-2" */
        String requestBody = String.format("""
            {
                "model": "sd3-large",
                "prompt": "%s",
                "n": 1,
                "size": "1024x1024",
                "quality": "standard"
            }
            """, prompt.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            String imageUrl = rootNode.path("data").get(0).path("url").asText();
            log.info("Изображение сгенерировано, URL: {}", imageUrl);
            return imageUrl;
        } else {
            log.error("Ошибка API. Код: {}, Тело: {}", response.statusCode(), response.body());
            throw new RuntimeException("API error: " + response.statusCode());
        }
    }

}
