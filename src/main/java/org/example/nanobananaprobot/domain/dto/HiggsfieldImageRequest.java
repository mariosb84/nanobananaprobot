package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HiggsfieldImageRequest {
    private String prompt;

    @JsonProperty("aspect_ratio")
    private String aspectRatio = "16:9";

    private String resolution = "720p";
}