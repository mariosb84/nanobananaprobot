package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.nanobananaprobot.domain.dto.CometApiResponse;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class CometApiService {

    @Value("${cometapi.url}")
    private String API_URL;

    @Value("${cometapi.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Добавьте логгер в начало класса
    private static final Logger log = LoggerFactory.getLogger(CometApiService.class);

    public CometApiService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public byte[] generateImage(String prompt, ImageConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Формируем запрос с учетом ImageConfig
        Map<String, Object> requestBody = createRequestBody(prompt, config);

        // ★ ДОБАВЬ ЛОГИРОВАНИЕ ЗАПРОСА
        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.info("=== API REQUEST ===");
            log.info("URL: {}", API_URL);
            log.info("Headers: {}", headers);
            log.info("Body: {}", requestJson);
            log.info("===================");
        } catch (Exception e) {
            log.error("Failed to log request", e);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        // ★ ДОБАВЬ ЛОГИРОВАНИЕ ОТВЕТА
        log.info("=== API RESPONSE ===");
        log.info("Status: {}", response.getStatusCode());
        log.info("Body preview: {}", response.getBody().substring(0, Math.min(500, response.getBody().length())));
        log.info("===================");

        return parseResponse(response.getBody());
    }

    public byte[] editImage(byte[] sourceImage, String prompt, ImageConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Формируем запрос для редактирования
        Map<String, Object> requestBody = createEditRequestBody(sourceImage, prompt, config);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        return parseResponse(response.getBody());
    }

    /**
     * Создание тела запроса для генерации
     */
    private Map<String, Object> createRequestBody(String prompt, ImageConfig config) {
        Map<String, Object> requestBody = new HashMap<>();

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        requestBody.put("contents", List.of(content));

        // ДЛЯ ПРОСТОЙ ГЕНЕРАЦИИ - добавляем aspectRatio и imageSize (качество)
        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, Object> imageConfig = new HashMap<>();

        // КОНВЕРТИРУЕМ и передаем aspectRatio
        String geminiAspectRatio = convertAspectRatio(config.getAspectRatio());
        imageConfig.put("aspectRatio", geminiAspectRatio);

        // ★ ВАЖНО: передаем качество (imageSize вместо resolution)
        // Поддерживаемые значения: "1K", "2K", "4K"
        imageConfig.put("imageSize", config.getResolution());

        generationConfig.put("imageConfig", imageConfig);
        generationConfig.put("responseModalities", List.of("IMAGE"));

        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Создание тела запроса для редактирования
     */
    private Map<String, Object> createEditRequestBody(byte[] sourceImage, String prompt, ImageConfig config) {
        Map<String, Object> requestBody = new HashMap<>();

        String base64Image = Base64.getEncoder().encodeToString(sourceImage);

        List<Map<String, Object>> parts = new ArrayList<>();

        // inline_data (snake_case) для совместимости с CometAPI
        parts.add(Map.of(
                "inline_data", Map.of(
                        "mime_type", "image/jpeg", // snake_case
                        "data", base64Image
                )
        ));

        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        requestBody.put("contents", List.of(content));

        // ДЛЯ РЕДАКТИРОВАНИЯ тоже передаем aspectRatio и imageSize
        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, Object> imageConfig = new HashMap<>();

        // КОНВЕРТИРУЕМ и передаем aspectRatio
        String geminiAspectRatio = convertAspectRatio(config.getAspectRatio());
        imageConfig.put("aspectRatio", geminiAspectRatio);

        // ★ ВАЖНО: передаем качество (imageSize вместо resolution)
        imageConfig.put("imageSize", config.getResolution());

        generationConfig.put("imageConfig", imageConfig);
        generationConfig.put("responseModalities", List.of("IMAGE"));

        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private byte[] parseResponse(String responseBody) {
        try {
            CometApiResponse response = objectMapper.readValue(responseBody, CometApiResponse.class);

            if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new RuntimeException("API не вернуло кандидатов");
            }

            var candidate = response.getCandidates().get(0);
            if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
                throw new RuntimeException("Неверная структура ответа");
            }

            for (var part : candidate.getContent().getParts()) {
                // Проверяем оба варианта
                CometApiResponse.InlineData data = part.getInlineData() != null ?
                        part.getInlineData() : part.getInline_data();

                if (data != null && data.getData() != null) {
                    String base64Image = data.getData();
                    return Base64.getDecoder().decode(base64Image);
                }
            }

            throw new RuntimeException("Изображение не найдено в ответе");

        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки ответа: " + e.getMessage(), e);
        }
    }

    // Добавьте этот метод если нужно конвертировать ваши настройки в формат Gemini
    private String convertAspectRatio(String userAspectRatio) {
        if (userAspectRatio == null) return "1:1";

        // Поддерживаемые Gemini значения: "1:1", "16:9", "3:4", "4:3"
        return switch (userAspectRatio) {
            case "16:9" -> "16:9";
            case "3:4" -> "3:4";
            case "4:3" -> "4:3";
            default -> "1:1";
        };
    }

}