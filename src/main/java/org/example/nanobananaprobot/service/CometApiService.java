package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.nanobananaprobot.domain.dto.CometApiResponse;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CometApiService {

    @Value("${cometapi.url}")
    private String API_URL;

    @Value("${cometapi.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CometApiService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Генерация изображения с настройками
     */
    public byte[] generateImage(String prompt, ImageConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Формируем запрос с учетом ImageConfig
        Map<String, Object> requestBody = createRequestBody(prompt, config);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        return parseResponse(response.getBody());
    }

    /**
     * Редактирование изображения (image-to-image)
     */
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

        // Текстовая часть
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> parts = Map.of("parts", List.of(textPart));
        Map<String, Object> contents = Map.of("contents", List.of(parts));

        requestBody.putAll(contents);

        // Конфигурация генерации
        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, String> imageConfig = new HashMap<>();

        imageConfig.put("aspectRatio", config.getAspectRatio());
        imageConfig.put("resolution", config.getResolution());

        generationConfig.put("imageConfig", imageConfig);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Создание тела запроса для редактирования
     */
    private Map<String, Object> createEditRequestBody(byte[] sourceImage, String prompt, ImageConfig config) {
        Map<String, Object> requestBody = new HashMap<>();

        // Кодируем исходное изображение в Base64
        String base64Image = Base64.getEncoder().encodeToString(sourceImage);

        // Создаем массив parts: [изображение, текст]
        List<Map<String, Object>> parts = new ArrayList<>();

        // Часть с изображением
        parts.add(Map.of(
                "inlineData", Map.of(
                        "mimeType", "image/jpeg",
                        "data", base64Image
                )
        ));

        // Часть с текстовым промптом
        parts.add(Map.of("text", prompt));

        Map<String, Object> contents = Map.of("contents", List.of(Map.of("parts", parts)));
        requestBody.putAll(contents);

        // Конфигурация генерации
        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, String> imageConfig = new HashMap<>();

        imageConfig.put("aspectRatio", config.getAspectRatio());
        imageConfig.put("resolution", config.getResolution());

        generationConfig.put("imageConfig", imageConfig);
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
            if (candidate.getContent() == null || candidate.getContent().getParts() == null ||
                    candidate.getContent().getParts().isEmpty()) {
                throw new RuntimeException("Неверная структура ответа");
            }

            var part = candidate.getContent().getParts().get(0);
            if (part.getInlineData() == null || part.getInlineData().getData() == null) {
                throw new RuntimeException("Изображение не найдено в ответе");
            }

            // Декодируем Base64 строку в байты
            return Base64.getDecoder().decode(part.getInlineData().getData());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга JSON ответа", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Ошибка декодирования Base64 строки", e);
        }
    }

}