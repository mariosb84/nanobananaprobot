package org.example.nanobananaprobot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.example.nanobananaprobot.domain.model.UserGenerationBalance;
import org.example.nanobananaprobot.repository.UserGenerationBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationBalanceService {

    private final UserGenerationBalanceRepository balanceRepository;
    private final CostCalculatorService costCalculatorService; // Будет создан позже

    /* ========== БАЗОВЫЕ МЕТОДЫ ДЛЯ ТОКЕНОВ ==========*/

    @Transactional
    public UserGenerationBalance getOrCreateBalance(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserGenerationBalance newBalance = new UserGenerationBalance();
                    newBalance.setUserId(userId);

                    /* НАЧАЛЬНЫЙ БАЛАНС: 0 токенов (без бесплатных)*/

                    newBalance.setTokensBalance(0);
                    newBalance.setImageBalance(0); /* Было 3*/
                    newBalance.setVideoBalance(0);
                    newBalance.setTrialUsed(true); /* Триал сразу использован*/
                    log.info("Создан баланс для нового пользователя userId: {}", userId);
                    return balanceRepository.save(newBalance);
                });
    }

    /**
     * Получить баланс токенов
     */
    public int getTokensBalance(Long userId) {
        return getOrCreateBalance(userId).getTokensBalance();
    }

    /**
     * Проверить, достаточно ли токенов
     */
    public boolean hasEnoughTokens(Long userId, int requiredTokens) {
        return getTokensBalance(userId) >= requiredTokens;
    }

    /**
     * Списывает токены (универсальный метод)
     */
    @Transactional
    public boolean useTokens(Long userId, int tokens) {
        UserGenerationBalance balance = getOrCreateBalance(userId);

        if (balance.getTokensBalance() >= tokens) {
            balance.setTokensBalance(balance.getTokensBalance() - tokens);
            balanceRepository.save(balance);

            log.info("✅ Списано {} токенов у userId: {}. Остаток: {}",
                    tokens, userId, balance.getTokensBalance());
            return true;
        }

        log.warn("❌ Недостаточно токенов у userId: {}. Требуется: {}, Есть: {}",
                userId, tokens, balance.getTokensBalance());
        return false;
    }

    /**
     * Добавить токены (при покупке пакета)
     */
    @Transactional
    public void addTokens(Long userId, int tokens) {
        UserGenerationBalance balance = getOrCreateBalance(userId);
        balance.setTokensBalance(balance.getTokensBalance() + tokens);
        balanceRepository.save(balance);

        log.info("✅ Добавлено {} токенов userId: {}. Баланс: {}",
                tokens, userId, balance.getTokensBalance());
    }

    /* ========== МЕТОДЫ ДЛЯ КОНКРЕТНЫХ ОПЕРАЦИЙ ==========*/

    /**
     * Списывает токены за генерацию изображения
     */
    @Transactional
    public boolean useImageGeneration(Long userId, ImageConfig config) {
        int requiredTokens = costCalculatorService.calculateTokens(config);
        return useTokens(userId, requiredTokens);
    }

    /**
     * Списывает токены за редактирование изображения
     */
    @Transactional
    public boolean useImageEdit(Long userId, ImageConfig config) {

        /* Для редактирования устанавливаем режим*/

        config.setMode("edit");
        int requiredTokens = costCalculatorService.calculateTokens(config);
        return useTokens(userId, requiredTokens);
    }

    /**
     * Списывает токены за слияние изображений
     */
    @Transactional
    public boolean useImageMerge(Long userId, ImageConfig config, int imageCount) {
        int requiredTokens = costCalculatorService.calculateMergeTokens(config, imageCount);
        return useTokens(userId, requiredTokens);
    }

    /**
     * Проверяет, достаточно ли токенов для генерации
     */
    public boolean canGenerateImage(Long userId, ImageConfig config) {
        int requiredTokens = costCalculatorService.calculateTokens(config);
        return hasEnoughTokens(userId, requiredTokens);
    }

    /**
     * Проверяет, достаточно ли токенов для редактирования
     */
    public boolean canEditImage(Long userId, ImageConfig config) {
        config.setMode("edit");
        int requiredTokens = costCalculatorService.calculateTokens(config);
        return hasEnoughTokens(userId, requiredTokens);
    }

    /**
     * Проверяет, достаточно ли токенов для слияния
     */
    public boolean canMergeImages(Long userId, ImageConfig config, int imageCount) {
        int requiredTokens = costCalculatorService.calculateMergeTokens(config, imageCount);
        return hasEnoughTokens(userId, requiredTokens);
    }

    /* ========== МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========*/

    /* (можно удалить позже, когда переведём всё на токены)*/

    /**
     * Старый метод для обратной совместимости
     * @deprecated Используйте useImageGeneration(userId, config)
     */
    @Deprecated
    @Transactional
    public boolean useImageGeneration(Long userId) {
        log.warn("Используется устаревший метод useImageGeneration без конфига");

        /* Можно вернуть false или попробовать использовать токены*/

        return false;
    }

    /**
     * Старый метод для обратной совместимости
     * @deprecated Используйте getTokensBalance(userId)
     */
    @Deprecated
    public Integer getImageBalance(Long userId) {
        return getOrCreateBalance(userId).getImageBalance();
    }

    /**
     * Старый метод для обратной совместимости
     * @deprecated Используйте canGenerateImage(userId, config)
     */
    @Deprecated
    public boolean canAffordGeneration(Long userId, double cost) {
        log.warn("Используется устаревший метод canAffordGeneration");

        /* Минимальная проверка: хотя бы 3 токена для генерации 1K*/

        return getTokensBalance(userId) >= 3;
    }

    /**
     * Старый метод для обратной совместимости
     * @deprecated Используйте addTokens(userId, count)
     */
    @Deprecated
    @Transactional
    public void addImageGenerations(Long userId, Integer count) {
        log.warn("Используется устаревший метод addImageGenerations");

        /* Конвертируем старые "генерации" в токены (1 генерация = 3 токена)*/

        addTokens(userId, count * 3);
    }

    /**
     * Возвращает токены при ошибке генерации
     */
    @Transactional
    public void refundTokens(Long userId, int tokens) {
        addTokens(userId, tokens);
        log.info("Возвращено {} токенов userId: {}", tokens, userId);
    }

    public Integer getVideoBalance(Long userId) {
        return getOrCreateBalance(userId).getVideoBalance();
    }

    /**
     * Старый метод для обратной совместимости
     * @deprecated Используйте addTokens(userId, count * 10)
     */
    @Deprecated
    @Transactional
    public void addVideoGenerations(Long userId, Integer count) {
        log.warn("Используется устаревший метод addVideoGenerations");

        /* Конвертируем старые "видео" в токены (1 видео = 10 токенов)*/

        addTokens(userId, count * 10);
    }

}