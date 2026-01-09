package org.example.nanobananaprobot.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Candidate {
    private Content content;
    private String finishReason;
    private int index;

}