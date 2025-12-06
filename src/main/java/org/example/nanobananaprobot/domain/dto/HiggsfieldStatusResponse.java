package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class HiggsfieldStatusResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String status; // "queued", "processing", "completed", "failed"

    @JsonProperty("status_url")
    private String statusUrl;

    @JsonProperty("cancel_url")
    private String cancelUrl;

    private List<ImageData> images;
    private VideoData video;

    @Data
    public static class ImageData {
        private String url;
    }

    @Data
    public static class VideoData {
        private String url;
    }
}