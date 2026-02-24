package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
/*import com.fasterxml.jackson.annotation.JsonProperty;*/
import java.util.List;

@Setter @Getter
@JsonIgnoreProperties(ignoreUnknown = true) /* ← ДОБАВЬ ЭТУ СТРОКУ*/
public class CometApiResponse {
    private List<Candidate> candidates;

    @Setter @Getter
    @JsonIgnoreProperties(ignoreUnknown = true) /* ← ДОБАВЬ ЭТУ СТРОКУ*/
    public static class Candidate {
        private Content content;
    }

    @Setter @Getter
    @JsonIgnoreProperties(ignoreUnknown = true) /* ← ДОБАВЬ ЭТУ СТРОКУ*/
    public static class Content {
        private List<Part> parts;
    }

    @Setter @Getter
    @JsonIgnoreProperties(ignoreUnknown = true) /* ← ДОБАВЬ ЭТУ СТРОКУ*/
    public static class Part {
        private String text;

        /*@JsonProperty("inline_data")*/ /* ← snake_case как в документации*/
        private InlineData inline_data;
        private InlineData inlineData; /* ← добавил второй вариант*/
    }

    @Setter @Getter
    @JsonIgnoreProperties(ignoreUnknown = true) /* ← ДОБАВЬ ЭТУ СТРОКУ*/
    public static class InlineData {
        /*@JsonProperty("mime_type")*/ /* ← snake_case*/
        private String mime_type;
        private String mimeType;

        private String data;
    }

}
