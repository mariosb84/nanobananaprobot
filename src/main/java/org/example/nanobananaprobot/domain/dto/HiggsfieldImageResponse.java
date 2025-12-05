package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HiggsfieldImageResponse {
    @JsonProperty("request_id")
    private String requestId;

    private String status;
    private String message;
}
