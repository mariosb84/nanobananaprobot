package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.nanobananaprobot.domain.dto.CometApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public byte[] generateImage(String prompt) {
        // 1. Формируем заголовки
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. Формируем тело запроса
        Map<String, Object> requestBody = createRequestBody(prompt);

        // 3. Отправляем запрос
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        // 4. Парсим ответ с помощью DTO
        return parseResponse(response.getBody());
    }

    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        // Создаем parts
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        // Создаем contents
        Map<String, Object> contents = new HashMap<>();
        contents.put("contents", List.of(parts));

        requestBody.putAll(contents);

        // Создаем generationConfig
        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, String> imageConfig = new HashMap<>();
        imageConfig.put("aspectRatio", "1:1");
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