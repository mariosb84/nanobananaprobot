package org.example.nanobananaprobot.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InlineData {
    private String mimeType;
    private String data; // Base64 строка

}