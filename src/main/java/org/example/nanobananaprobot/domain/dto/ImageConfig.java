package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

@Data
public class ImageConfig {
    // ========== ОСНОВНЫЕ ПОЛЯ ==========
    private String aspectRatio = "1:1";
    private String resolution = "1K";
    private String mode = "generate"; // "generate", "edit", "merge"
    private byte[] sourceImage;

    // ========== ВАЛИДНЫЕ ЗНАЧЕНИЯ (только для информации) ==========
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
    public static final String[] VALID_MODES = {"generate", "edit", "merge"};

    // ========== КОНСТАНТЫ УБРАНЫ (теперь в CostCalculatorService) ==========

    // ========== ТОЛЬКО ПРОСТАЯ ВАЛИДАЦИЯ ==========

    /**
     * Базовая проверка валидности (только типы)
     */
    public boolean isValid() {
        return isValidAspectRatio(aspectRatio) &&
                isValidResolution(resolution) &&
                isValidMode(mode);
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

    private boolean isValidMode(String m) {
        for (String validMode : VALID_MODES) {
            if (validMode.equals(m)) return true;
        }
        return false;
    }

    // ========== МЕТОДЫ РАСЧЁТА УБРАНЫ ==========
    // calculateCost(), calculateMergeCost(), getDescription(), getMergeDescription()
    // теперь в CostCalculatorService

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Быстрая проверка, является ли режимом слияния
     */
    public boolean isMergeMode() {
        return "merge".equals(mode);
    }

    /**
     * Быстрая проверка, является ли режимом редактирования
     */
    public boolean isEditMode() {
        return "edit".equals(mode);
    }

    /**
     * Быстрая проверка, является ли режимом генерации
     */
    public boolean isGenerateMode() {
        return "generate".equals(mode) || mode == null;
    }

    /**
     * Простое строковое представление (без стоимости)
     */
    public String toSimpleString() {
        return String.format("%s | %s", aspectRatio, resolution);
    }
}