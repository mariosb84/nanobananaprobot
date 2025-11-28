package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) /* ИГНОРИРУЕМ неизвестные поля*/
public class PaymentCreateResponse {
    private String id;
    private String status;
    private String description; /* Добавляем недостающее поле*/
    private Amount amount;
    private Confirmation confirmation;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private String value;
        private String currency;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Confirmation {
        private String type;

        @JsonProperty("confirmation_url")
        private String confirmationUrl;
    }

}
