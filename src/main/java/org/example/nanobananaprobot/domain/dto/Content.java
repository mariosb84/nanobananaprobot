package org.example.nanobananaprobot.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Content {
    private List<Part> parts;
    private String role;

}