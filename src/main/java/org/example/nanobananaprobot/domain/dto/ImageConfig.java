package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

@Data
public class ImageConfig {
    private String aspectRatio = "1:1";
    private String resolution = "1K";
    private String mode = "generate"; // "generate", "edit", "merge"
    private byte[] sourceImage;

    // БАЗОВЫЕ СТОИМОСТИ
    private static final double COST_GENERATE_1K = 0.11;
    private static final double COST_EDIT_1K = 0.15;
    private static final double COST_MERGE_1K = 0.18;

    // МНОЖИТЕЛИ
    private static final double MULTIPLIER_2K = 1.5;
    private static final double MULTIPLIER_4K = 2.2;
    private static final double MULTIPLIER_PER_EXTRA_IMAGE = 0.12;

    // ВАЖНО: Полный список форматов, поддерживаемых Nano Banana Pro
    public static final String[] VALID_ASPECT_RATIOS = {
            "1:1",      // Квадрат
            "4:3",      // Классический
            "3:4",      // Портрет
            "16:9",     // Широкий
            "9:16",     // Сторис
            "3:2",      // Фотография
            "2:3",      // Портрет 2:3
            "5:4",      // 5:4
            "4:5",      // 4:5
            "21:9"      // Ультраширокий
    };

    public static final String[] VALID_RESOLUTIONS = {"1K", "2K", "4K"};

    /**
     * Проверка валидности настроек
     */
    public boolean isValid() {
        return isValidAspectRatio(aspectRatio) && isValidResolution(resolution);
    }

    private boolean isValidAspectRatio(String ratio) {
        for (String validRatio : VALID_ASPECT_RATIOS) {
            if (validRatio.equals(ratio)) return true;
        }
        return false;
    }

    private boolean isValidResolution(String res) {
        for (String validRes : VALID_RESOLUTIONS) {
            if (validRes.equals(res)) return true;
        }
        return false;
    }

    /**
     * Рассчитывает стоимость на основе настроек
     */
    public double calculateCost() {
        double base;

        switch (mode) {
            case "edit":
                base = COST_EDIT_1K;
                break;
            case "merge":
                base = COST_MERGE_1K;
                break;
            case "generate":
            default:
                base = COST_GENERATE_1K;
        }

        if ("2K".equals(resolution)) {
            return base * MULTIPLIER_2K;
        } else if ("4K".equals(resolution)) {
            return base * MULTIPLIER_4K;
        }

        return base; // 1K
    }

    /**
     * Рассчитывает стоимость слияния
     */
    public double calculateMergeCost(int imageCount) {
        if (!"merge".equals(mode)) {
            return calculateCost();
        }

        double baseCost = calculateCost();

        if (imageCount < 2) {
            return baseCost;
        }

        double multiplier = 1.0 + (imageCount - 1) * MULTIPLIER_PER_EXTRA_IMAGE;
        return baseCost * multiplier;
    }

    /**
     * Возвращает человеко-читаемое описание
     */
    public String getDescription() {
        return String.format("%s | %s | %.2f$", aspectRatio, resolution, calculateCost());
    }

    /**
     * Описание для слияния
     */
    public String getMergeDescription(int imageCount) {
        return String.format("%d фото | %s | %s | %.2f$",
                imageCount, aspectRatio, resolution, calculateMergeCost(imageCount));
    }

}
