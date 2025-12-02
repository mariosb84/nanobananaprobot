package org.example.nanobananaprobot.bot.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Map;

@Data
@Service
@ConfigurationProperties(prefix = "app.packages")
public class PackageService {
    private Map<String, String> image;  /* "3" -> "39", "10" -> "99", и т.д.*/
    private Map<String, String> video;

    public String getImagePackagePrice(String count) {
        return image.getOrDefault(count, "0");
    }

    public String getVideoPackagePrice(String count) {
        return video.getOrDefault(count, "0");
    }

    public String getPricePerGeneration(String count, String totalPrice) {
        double price = Double.parseDouble(totalPrice);
        double countNum = Double.parseDouble(count);
        return String.format("%.0f", price / countNum);
    }

}