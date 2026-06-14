package org.example.nanobananaprobot.service;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
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

    /* Добавьте логгер в начало класса*/

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

        /* Формируем запрос с учетом ImageConfig*/

        Map<String, Object> requestBody = createRequestBody(prompt, config);

        /* ★ ДОБАВЬ ЛОГИРОВАНИЕ ЗАПРОСА*/

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

        /* ★ ДОБАВЬ ЛОГИРОВАНИЕ ОТВЕТА*/

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

        /* Формируем запрос для редактирования*/

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

        /* ДЛЯ ПРОСТОЙ ГЕНЕРАЦИИ - добавляем aspectRatio и imageSize (качество)*/

        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, Object> imageConfig = new HashMap<>();

        /* КОНВЕРТИРУЕМ и передаем aspectRatio*/

        String geminiAspectRatio = convertAspectRatio(config.getAspectRatio());
        imageConfig.put("aspectRatio", geminiAspectRatio);

        /* ★ ВАЖНО: передаем качество (imageSize вместо resolution)
         Поддерживаемые значения: "1K", "2K", "4K"*/

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

        /* inline_data (snake_case) для совместимости с CometAPI*/

        parts.add(Map.of(
                "inline_data", Map.of(
                        "mime_type", "image/jpeg", /* snake_case*/
                        "data", base64Image
                )
        ));

        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        requestBody.put("contents", List.of(content));

        /* ДЛЯ РЕДАКТИРОВАНИЯ тоже передаем aspectRatio и imageSize*/

        Map<String, Object> generationConfig = new HashMap<>();
        Map<String, Object> imageConfig = new HashMap<>();

        /* КОНВЕРТИРУЕМ и передаем aspectRatio*/

        String geminiAspectRatio = convertAspectRatio(config.getAspectRatio());
        imageConfig.put("aspectRatio", geminiAspectRatio);

        /* ★ ВАЖНО: передаем качество (imageSize вместо resolution)*/

        imageConfig.put("imageSize", config.getResolution());

        generationConfig.put("imageConfig", imageConfig);
        generationConfig.put("responseModalities", List.of("IMAGE"));

        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private byte[] parseResponse(String responseBody) {
        try {

            /* Добавь эти 3 строки:*/

            objectMapper.getFactory()
                    .setStreamReadConstraints(StreamReadConstraints.builder()
                            .maxStringLength(50_000_000) /* Увеличиваем до 50 млн*/
                            .build()
                    );

            CometApiResponse response = objectMapper.readValue(responseBody, CometApiResponse.class);

            if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
                throw new RuntimeException("API не вернуло кандидатов");
            }

            var candidate = response.getCandidates().get(0);
            if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
                throw new RuntimeException("Неверная структура ответа");
            }

            for (var part : candidate.getContent().getParts()) {

                /* Проверяем оба варианта*/
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

    /* Добавьте этот метод если нужно конвертировать ваши настройки в формат Gemini*/

    private String convertAspectRatio(String userAspectRatio) {
        if (userAspectRatio == null) return "1:1";

        /* Поддерживаемые значения Nano Banana Pro (Gemini 3 Pro Image)[citation:4][citation:10]*/

        return switch (userAspectRatio) {

            /* Квадрат*/

            case "1:1" -> "1:1";

            /* Альбомные (ландшафт)*/

            case "21:9" -> "21:9";
            case "16:9" -> "16:9";
            case "4:3" -> "4:3";
            case "3:2" -> "3:2";
            case "5:4" -> "5:4";

            /* Портретные*/

            case "9:16" -> "9:16";
            case "3:4" -> "3:4";
            case "2:3" -> "2:3";
            case "4:5" -> "4:5";

            /* По умолчанию используем квадрат*/

            default -> "1:1";
        };
    }

    /**
     * Объединение нескольких изображений по промпту
     */
    public byte[] mergeImages(List<byte[]> images, String prompt, ImageConfig config) {
        try {
            log.info("Запрос на слияние {} изображений через CometAPI", images.size());

            /* Формируем тело запроса без JSONObject/JSONArray */

            Map<String, Object> requestBody = new LinkedHashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            /* Добавляем текстовый промпт*/
            parts.add(Map.of("text", prompt));

            /* Добавляем все изображения*/
            for (byte[] image : images) {
                parts.add(Map.of(
                        "inlineData", Map.of(
                                "mimeType", "image/jpeg",
                                "data", Base64.getEncoder().encodeToString(image)
                        )
                ));
            }

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("parts", parts);
            requestBody.put("contents", List.of(content));

            /* Добавляем конфигурацию генерации*/
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("responseModalities", List.of("IMAGE"));

            Map<String, Object> imageConfigMap = new LinkedHashMap<>();
            imageConfigMap.put("aspectRatio", config.getAspectRatio());
            imageConfigMap.put("imageSize", config.getResolution());
            generationConfig.put("imageConfig", imageConfigMap);

            requestBody.put("generationConfig", generationConfig);

            /* Отправляем запрос*/
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestBodyStr, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            /* Обработка ответа*/
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.get("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonNode contentNode = candidates.get(0).get("content");
                    JsonNode partsArray = contentNode.get("parts");
                    for (JsonNode part : partsArray) {
                        if (part.has("inlineData")) {
                            String base64Image = part.get("inlineData").get("data").asText();
                            return Base64.getDecoder().decode(base64Image);
                        }
                    }
                }
                throw new RuntimeException("В ответе нет изображения");
            } else {
                throw new RuntimeException("Ошибка API: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Ошибка при слиянии изображений через CometAPI", e);
            throw new RuntimeException("Не удалось объединить изображения: " + e.getMessage(), e);
        }
    }

}
