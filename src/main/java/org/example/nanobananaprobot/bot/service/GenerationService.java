package org.example.nanobananaprobot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.HiggsfieldImageService;
import org.example.nanobananaprobot.service.ProxyApiImageService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    // Заменяем старый HiggsfieldAIService на новый
    private final ProxyApiImageService proxyApiImageService;
    private final UserServiceData userService;
    private final GenerationBalanceService balanceService;
    private final TelegramService telegramService;
    private final UserStateManager stateManager;
    private final HiggsfieldImageService higgsfieldImageService;

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

    /**
     * Асинхронная генерация изображения через DALL-E 3
     * Метод выполняется в отдельном потоке
     */
    @Async
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
