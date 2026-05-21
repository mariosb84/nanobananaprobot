package org.example.nanobananaprobot.domain.dto;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class TokenConfig {

    @Value("${token.price.rub:5}")
    private int priceRub;
}