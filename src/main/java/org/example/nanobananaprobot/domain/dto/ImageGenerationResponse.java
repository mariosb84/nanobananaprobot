package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerationResponse {
    private String status;
    private List<String> images;  /* URL сгенерированных изображений*/
    private String error;
    private String requestId;
}