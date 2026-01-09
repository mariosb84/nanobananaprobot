package org.example.nanobananaprobot.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UsageMetadata {
    private int promptTokenCount;
    private int candidatesTokenCount;
    private int totalTokenCount;
    private List<TokenDetails> promptTokensDetails;
    private List<TokenDetails> candidatesTokensDetails;
    private int thoughtsTokenCount;

}

@Setter
@Getter
class TokenDetails {
    private String modality;
    private int tokenCount;

}
