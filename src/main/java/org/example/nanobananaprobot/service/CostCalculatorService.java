package org.example.nanobananaprobot.service;

import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CostCalculatorService {

    /* ========== ЦЕНЫ В ТОКЕНАХ (1 токен = 5 ₽) ==========
     По расчётам: 1K=3т(15₽), 2K=4т(20₽), 4K=5т(25₽)*/

    /* ГЕНЕРАЦИЯ ИЗОБРАЖЕНИЙ*/

    private static final int TOKENS_GENERATE_1K = 3;   /* 15 ₽*/
    private static final int TOKENS_GENERATE_2K = 4;   /* 20 ₽*/
    private static final int TOKENS_GENERATE_4K = 5;   /* 25 ₽*/

    /* РЕДАКТИРОВАНИЕ (стоимость выше генерации)*/

    private static final int TOKENS_EDIT_1K = 4;       /* 20 ₽ (+1 токен)*/
    private static final int TOKENS_EDIT_2K = 5;       /* 25 ₽ (+1 токен)*/
    private static final int TOKENS_EDIT_4K = 6;       /* 30 ₽ (+1 токен)*/

    /* СЛИЯНИЕ (базовая стоимость)*/

    private static final int TOKENS_MERGE_BASE_1K = 5;    /* 25 ₽*/
    private static final int TOKENS_MERGE_BASE_2K = 6;    /* 30 ₽*/
    private static final int TOKENS_MERGE_BASE_4K = 7;    /* 35 ₽*/

    /* ДОПОЛНИТЕЛЬНЫЕ ИЗОБРАЖЕНИЯ*/

    private static final int TOKENS_PER_EXTRA_IMAGE = 1; /* +5 ₽ за каждое доп. фото*/

    private static final Logger log = LoggerFactory.getLogger(CostCalculatorService.class);

    /* ========== ОСНОВНЫЕ МЕТОДЫ РАСЧЁТА ==========*/

    /**
     * Рассчитывает стоимость операции в токенах
     */
    public int calculateTokens(ImageConfig config) {
        if (config == null) {
            log.info("calculateTokens: config is null, returning default 3");
            return TOKENS_GENERATE_1K;
        }

        log.info("calculateTokens: mode={}, resolution={}",
                config.getMode(), config.getResolution());

        int result = switch (config.getMode()) {
            case "edit" -> calculateEditTokens(config.getResolution());
            case "merge" -> calculateMergeTokens(config, 1);
            default -> calculateGenerateTokens(config.getResolution());
        };

        log.info("calculateTokens: result={} tokens", result);
        return result;
    }

    /**
     * Рассчитывает стоимость слияния в токенах
     * @param imageCount количество изображений (минимум 2)
     */
    public int calculateMergeTokens(ImageConfig config, int imageCount) {
        if (imageCount < 2) {
            imageCount = 2; /* Минимум 2 фото для слияния*/
        }

        int baseTokens = getMergeBaseTokens(config.getResolution());

        /* +1 токен за каждое дополнительное фото (начиная с 3-го)*/
        int extraTokens = Math.max(0, (imageCount - 2)) * TOKENS_PER_EXTRA_IMAGE;

        return baseTokens + extraTokens;
    }

    /* ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========*/

    /**
     * Токены для генерации
     */
    private int calculateGenerateTokens(String resolution) {
        if (resolution == null) return TOKENS_GENERATE_1K;

        return switch (resolution) {
            case "2K" -> TOKENS_GENERATE_2K;
            case "4K" -> TOKENS_GENERATE_4K;
            default -> TOKENS_GENERATE_1K; /* 1K или неизвестное*/
        };
    }

    /**
     * Токены для редактирования
     */
    private int calculateEditTokens(String resolution) {
        if (resolution == null) return TOKENS_EDIT_1K;

        return switch (resolution) {
            case "2K" -> TOKENS_EDIT_2K;
            case "4K" -> TOKENS_EDIT_4K;
            default -> TOKENS_EDIT_1K;
        };
    }

    /**
     * Базовая стоимость слияния
     */
    private int getMergeBaseTokens(String resolution) {
        if (resolution == null) return TOKENS_MERGE_BASE_1K;

        return switch (resolution) {
            case "2K" -> TOKENS_MERGE_BASE_2K;
            case "4K" -> TOKENS_MERGE_BASE_4K;
            default -> TOKENS_MERGE_BASE_1K;
        };
    }

    // ========== МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ ==========

    /**
     * Возвращает описание стоимости для генерации/редактирования
     */
    public String getDescription(ImageConfig config) {
        int tokens = calculateTokens(config);
        int rubles = tokensToRubles(tokens);

        return String.format("%s | %s | %d токенов (%d ₽)",
                config.getAspectRatio(), config.getResolution(), tokens, rubles);
    }

    /**
     * Возвращает описание стоимости для слияния
     */
    public String getMergeDescription(ImageConfig config, int imageCount) {
        int tokens = calculateMergeTokens(config, imageCount);
        int rubles = tokensToRubles(tokens);

        return String.format("%d фото | %s | %s | %d токенов (%d ₽)",
                imageCount, config.getAspectRatio(), config.getResolution(), tokens, rubles);
    }

    /**
     * Конвертирует токены в рубли
     */
    public int tokensToRubles(int tokens) {
        return tokens * 5; /* 1 токен = 5 ₽*/
    }

    /**
     * Конвертирует рубли в токены (при покупке пакетов)
     */
    public int rublesToTokens(int rubles) {
        return rubles / 5; /* 5 ₽ = 1 токен*/
    }

    /* ========== МЕТОДЫ ДЛЯ ПАКЕТОВ ТОКЕНОВ ==========*/

    /**
     * Рассчитывает количество токенов для пакета
     * Пакеты: 5, 10, 30, 50, 100 токенов
     */
    public int getTokensForPackage(String packageName) {
        if (packageName == null) return 0;

        return switch (packageName.toLowerCase()) {
            case "5 токенов" -> 5;
            case "10 токенов" -> 10;
            case "30 токенов" -> 30;
            case "50 токенов" -> 50;
            case "100 токенов" -> 100;
            default -> 0;
        };
    }

    /**
     * Рассчитывает стоимость пакета в рублях
     */
    public int getPackagePriceInRubles(String packageName) {
        int tokens = getTokensForPackage(packageName);
        return tokensToRubles(tokens);
    }

    /**
     * Рассчитывает стоимость пакета в токенах для отображения
     */
    public String getPackageDescription(String packageName) {
        int tokens = getTokensForPackage(packageName);
        int rubles = tokensToRubles(tokens);

        return String.format("%d токенов - %d ₽", tokens, rubles);
    }

}
