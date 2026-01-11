package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

@Data
public class ImageConfig {
    // Основные настройки
    private String aspectRatio = "1:1";  // Соотношение сторон
    private String resolution = "1K";    // Разрешение: 1K, 2K, 4K

    // Режим работы (если нужно в будущем)
    private String mode = "generate";    // "generate" или "edit"
    private byte[] sourceImage;          // Изображение для редактирования (если mode="edit")

    // Константы для расчёта стоимости
    private static final double BASE_COST_1K = 0.11;   // Базовая цена за 1K
    private static final double MULTIPLIER_2K = 1.8;   // Коэффициент для 2K
    private static final double MULTIPLIER_4K = 3.2;   // Коэффициент для 4K

    /**
     * Рассчитывает стоимость генерации на основе выбранного разрешения
     */
    public double calculateCost() {
        return switch (resolution) {
            case "2K" -> BASE_COST_1K * MULTIPLIER_2K;
            case "4K" -> BASE_COST_1K * MULTIPLIER_4K;
            default -> BASE_COST_1K; // 1K по умолчанию
        };
    }

    /**
     * Возвращает человеко-читаемое описание настроек
     */
    public String getDescription() {
        return String.format("%s | %s | %.2f$", aspectRatio, resolution, calculateCost());
    }
}