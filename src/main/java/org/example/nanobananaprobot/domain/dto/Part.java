package org.example.nanobananaprobot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // Добавляем эту аннотацию!
public class Part {
    private InlineData inlineData;
    private Object thoughtSignature; // Добавляем новое поле (может быть null)

}