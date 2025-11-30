package org.example.nanobananaprobot.service;

import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.config.HiggsfieldConfig;
import org.example.nanobananaprobot.domain.dto.ImageGenerationRequest;
import org.example.nanobananaprobot.domain.dto.ImageGenerationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@Slf4j
public class HiggsfieldAIService {

    private final WebClient webClient;
    private final HiggsfieldConfig config;

    public HiggsfieldAIService(HiggsfieldConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getUrl())
                .defaultHeader("Authorization", "Bearer " + config.getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public ImageGenerationResponse generateImage(String prompt) {
        return generateImage(new ImageGenerationRequest(prompt));
    }

    public ImageGenerationResponse generateImage(ImageGenerationRequest request) {
        try {
            log.info("Generating image with prompt: {}", request.getPrompt());

            ImageGenerationResponse response = webClient.post()
                    .uri("/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ImageGenerationResponse.class)
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .block();

            log.info("Image generation completed: {}", response.getStatus());
            return response;

        } catch (Exception e) {
            log.error("Error generating image: {}", e.getMessage());
            ImageGenerationResponse errorResponse = new ImageGenerationResponse();
            errorResponse.setStatus("error");
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }

    /* Метод для проверки доступности API*/
    public boolean isApiAvailable() {
        try {
            webClient.get()
                    .uri("/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}