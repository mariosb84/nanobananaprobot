package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

@Data
public class ImageConfig {
    private String aspectRatio = "1:1";
    private String resolution = "1K"; // "1K", "2K", "4K" - теперь передается в API как imageSize

    private String mode = "generate";
    private byte[] sourceImage;

    // Базовая стоимость для 1K
    private static final double COST_GENERATE_1K = 0.11;
    private static final double COST_EDIT_1K = 0.15;

    // Множители для разных разрешений
    private static final double MULTIPLIER_2K = 1.5;
    private static final double MULTIPLIER_4K = 2.2;

    public double calculateCost() {
        double base = "edit".equals(mode) ? COST_EDIT_1K : COST_GENERATE_1K;

        // Расчёт стоимости на основе разрешения
        if ("2K".equals(resolution)) {
            return base * MULTIPLIER_2K;
        } else if ("4K".equals(resolution)) {
            return base * MULTIPLIER_4K;
        }

        return base; // 1K
    }

    public String getDescription() {
        return String.format("%s | %s | %.2f$", aspectRatio, resolution, calculateCost());
    }
}