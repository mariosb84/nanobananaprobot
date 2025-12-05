package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HiggsfieldStatusResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String status; // "queued", "processing", "completed", "failed"

    private ResultData result;

    @Data
    public static class ResultData {
        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("video_url")
        private String videoUrl;

        @JsonProperty("error_message")
        private String errorMessage;
    }

}