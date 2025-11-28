package org.example.nanobananaprobot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageGenerationRequest {
    private String model = "sd3";  // По умолчанию Stable Diffusion 3
    private String prompt;
    private Integer width = 1024;
    private Integer height = 1024;
    private Integer numOutputs = 1;

    // Конструктор для быстрого создания
    public ImageGenerationRequest(String prompt) {
        this.prompt = prompt;
    }
}