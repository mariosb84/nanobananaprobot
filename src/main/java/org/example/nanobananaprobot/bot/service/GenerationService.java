package org.example.nanobananaprobot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final ProxyApiImageService proxyApiImageService;
    private final UserServiceData userService;
    private final GenerationBalanceService balanceService;
    private final TelegramService telegramService;
    private final UserStateManager stateManager;
    private final HiggsfieldImageService higgsfieldImageService;

    private final CometApiService cometApiService;

    @Transactional
    public void handleImageGeneration(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        // 1. Проверяем баланс
        if (balanceService.getImageBalance(user.getId()) <= 0) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций!\n\n" +
                            "🎨 Баланс: 0 изображений\n" +
                            "🛒 Купите пакет генераций в магазине"
            );
            return;
        }

        // 2. Списываем баланс
        boolean used = balanceService.useImageGeneration(user.getId());
        if (!used) {
            telegramService.sendMessage(chatId, "❌ Ошибка списания баланса");
            return;
        }

        // 3. Меняем состояние и уведомляем пользователя
        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId,
                "🎨 Генерирую изображение...\n\n" +
                        "📝 Промпт: _" + prompt + "_\n" +
                        "⏱️ Это займет ~20 секунд"
        );

        // 4. Запускаем асинхронную генерацию
        startAsyncGeneration(chatId, user.getId(), prompt);
    }

    /*/**
     * Асинхронная генерация изображения через DALL-E 3
     * Метод выполняется в отдельном потоке
     */
   /* @Async
    public void startAsyncGeneration(Long chatId, Long userId, String prompt) {
        try {
            log.info("Начало генерации для chatId: {}, prompt: {}", chatId, prompt);

            // 5. Вызов реального API через прокси
            String imageUrl = proxyApiImageService.generateImage(prompt);
            int newBalance = balanceService.getImageBalance(userId);

            // 6. Отправка успешного результата
            telegramService.sendMessage(chatId,
                    "✅ Изображение готово!\n\n" +
                            "🖼️ Ссылка: " + imageUrl + "\n" +
                            "📝 Промпт: _" + prompt + "_\n" +
                            "🎨 Осталось генераций: " + newBalance
            );

            log.info("Генерация успешна для chatId: {}, URL: {}", chatId, imageUrl);

        } catch (Exception e) {
            log.error("Ошибка генерации для chatId: {}", chatId, e);

            // 7. Возвращаем баланс при ошибке
            try {
                balanceService.addImageGenerations(userId, 1);
                log.info("Баланс возвращен для userId: {}", userId);
            } catch (Exception ex) {
                log.error("Не удалось вернуть баланс для userId: {}", userId, ex);
            }

            // 8. Уведомляем пользователя об ошибке
            telegramService.sendMessage(chatId,
                    "❌ Произошла ошибка при генерации\n\n" +
                            "🎨 Баланс возвращен\n" +
                            "⚠️ Попробуйте позже или измените запрос"
            );
        } finally {
            // 9. Возвращаем пользователя в главное меню
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
        }
    }*/

    /**
     * Асинхронная генерация изображения через Nano Banana Pro (CometAPI)
     * Метод выполняется в отдельном потоке
     */
    @Async
    public void startAsyncGeneration(Long chatId, Long userId, String prompt) {
        try {
            log.info("Начало генерации через CometAPI для chatId: {}, prompt: {}", chatId, prompt);

            // 1. Получаем настройки пользователя
            ImageConfig config = stateManager.getOrCreateConfig(chatId); // Добавьте эту строку

            // 2. Вызов нового API Comet (Nano Banana Pro) с настройками
            byte[] imageBytes = cometApiService.generateImage(prompt, config); // Добавьте config

            // 3. Получаем актуальный баланс после успешной генерации
            int newBalance = balanceService.getImageBalance(userId);

            // 4. Отправляем САМО ИЗОБРАЖЕНИЕ в Telegram (а не ссылку)

            /*telegramService.sendPhoto(chatId, imageBytes, "generated_image.jpg");*/

            // ★ Умная отправка с автовыбором
            telegramService.sendImageSmart(chatId, imageBytes, "image.jpg", config);

            // 5. Отправляем текстовое подтверждение с информацией о настройках
            telegramService.sendMessage(chatId,
                    "✅ Изображение готово!\n\n" +
                            "📝 Промпт: _" + prompt + "_\n" +
                            "⚙️ Настройки: " + config.getDescription() + "\n" + // Добавлено
                            "🎨 Осталось генераций: " + newBalance
            );

            log.info("Генерация через CometAPI успешна для chatId: {}, размер изображения: {} байт",
                    chatId, imageBytes.length);

        } catch (Exception e) {
            log.error("Ошибка генерации через CometAPI для chatId: {}", chatId, e);

            // 6. Возвращаем баланс при ошибке
            try {
                balanceService.addImageGenerations(userId, 1);
                log.info("Баланс возвращен для userId: {} после ошибки CometAPI", userId);
            } catch (Exception ex) {
                log.error("Не удалось вернуть баланс для userId: {}", userId, ex);
            }

            // 7. Уведомляем пользователя об ошибке
            String errorMessage = "❌ Произошла ошибка при генерации изображения\n\n" +
                    "🎨 Баланс возвращен\n" +
                    "⚠️ " + getErrorMessage(e);

            telegramService.sendMessage(chatId, errorMessage);
        } finally {
            // 8. Возвращаем пользователя в главное меню
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
        }
    }

    /**
     * Вспомогательный метод для форматирования сообщений об ошибках
     */
    private String getErrorMessage(Exception e) {
        if (e.getMessage().contains("quota") || e.getMessage().contains("balance")) {
            return "Проверьте баланс аккаунта CometAPI";
        } else if (e.getMessage().contains("timeout") || e.getMessage().contains("connection")) {
            return "Проблемы с подключением к сервису, попробуйте позже";
        } else {
            return "Попробуйте изменить запрос или повторить позже";
        }
    }

    /**
     * Обработка генерации видео
     * TODO: Реализовать после настройки изображений
     */
    public void handleVideoGeneration(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        // Проверяем баланс видео
        if (balanceService.getVideoBalance(user.getId()) <= 0) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций видео!\n\n" +
                            "🎥 Баланс: 0 видео\n" +
                            "🛒 Купите пакет видео в магазине"
            );
            return;
        }

        telegramService.sendMessage(chatId,
                "🎥 Генерация видео через DALL-E 3 пока в разработке...\n\n" +
                        "📝 Ваш запрос сохранен: _" + prompt + "_\n\n" +
                        "⚠️ Функция появится в ближайшее время"
        );

        // TODO: Реализовать списание видео-баланса
        // boolean used = balanceService.useVideoGeneration(user.getId());
    }

    @Transactional
    public void testHiggsfieldGeneration(Long chatId, String prompt) {
        try {
            telegramService.sendMessage(chatId, "🧪 Тестирую Higgsfield...");

            // Временный вызов Higgsfield вместо DALL-E 3
            String imageUrl = higgsfieldImageService.generateImage(prompt);

            telegramService.sendMessage(chatId,
                    "✅ Higgsfield работает!\n\n" +
                            "🖼️ Ссылка: " + imageUrl + "\n" +
                            "📝 Промпт: _" + prompt + "_"
            );

        } catch (Exception e) {
            log.error("Тест Higgsfield не удался", e);
            telegramService.sendMessage(chatId,
                    "❌ Higgsfield ошибка: " + e.getMessage() + "\n\n" +
                            "🔄 Продолжаю использовать DALL-E 3"
            );
        }
    }

}
