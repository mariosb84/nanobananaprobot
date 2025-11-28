package org.example.nanobananaprobot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "higgsfield.api")
@Data
public class HiggsfieldConfig {
    private String key;
    private String url;
    private int timeout;
}
