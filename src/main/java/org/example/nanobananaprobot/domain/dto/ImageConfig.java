package org.example.nanobananaprobot.domain.dto;

import lombok.Data;

@Data
public class ImageConfig {

    private String aspectRatio = "1:1";  // Соотношение сторон

    // Убираем или оставляем только для интерфейса, но не для API
    private String resolution = "1K";    // Только для отображения пользователю

    // Режим работы
    private String mode = "generate";
    private byte[] sourceImage;

    // Константы для расчёта стоимости (теперь только на основе mode)
    private static final double BASE_COST_GENERATE = 0.11;
    private static final double BASE_COST_EDIT = 0.15; // Редактирование дороже

    /**
     * Рассчитывает стоимость на основе типа операции
     */
    public double calculateCost() {
        return "edit".equals(mode) ? BASE_COST_EDIT : BASE_COST_GENERATE;
    }

    /**
     * Возвращает описание настроек
     */
    public String getDescription() {
        return String.format("%s | %.2f$", aspectRatio, calculateCost());
    }

}