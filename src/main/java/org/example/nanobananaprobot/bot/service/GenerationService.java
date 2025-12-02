package org.example.nanobananaprobot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.HiggsfieldAIService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final HiggsfieldAIService higgsfieldService;
    private final UserServiceData userService;
    private final GenerationBalanceService balanceService;
    private final TelegramService telegramService;
    private final UserStateManager stateManager;

    public void handleImageGeneration(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Проверяем баланс изображений*/
        if (balanceService.getImageBalance(user.getId()) <= 0) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций!\n\n" +
                            "🎨 Баланс: 0 изображений\n" +
                            "🛒 Купите пакет генераций в магазине"
            );
            return;
        }

        /* Используем одну генерацию*/
        boolean used = balanceService.useImageGeneration(user.getId());
        if (!used) {
            telegramService.sendMessage(chatId, "❌ Ошибка списания баланса");
            return;
        }

        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId,
                "🎨 Генерирую изображение...\n\n" +
                        "📝 Промпт: _" + prompt + "_\n" +
                        "⏱️ Это займет ~30 секунд"
        );

        CompletableFuture.runAsync(() -> {
            try {
                var response = higgsfieldService.generateImage(prompt);

                if ("success".equals(response.getStatus()) &&
                        response.getImages() != null &&
                        !response.getImages().isEmpty()) {

                    String imageUrl = response.getImages().get(0);
                    int newBalance = balanceService.getImageBalance(user.getId());

                    telegramService.sendMessage(chatId,
                            "✅ Изображение готово!\n\n" +
                                    "🖼️ Ссылка: " + imageUrl + "\n" +
                                    "📝 Промпт: _" + prompt + "_\n" +
                                    "🎨 Осталось генераций: " + newBalance
                    );

                } else {
                    /* Возвращаем генерацию если ошибка*/
                    balanceService.addImageGenerations(user.getId(), 1);
                    telegramService.sendMessage(chatId,
                            "❌ Ошибка генерации: " + response.getError() + "\n" +
                                    "🎨 Баланс возвращен"
                    );
                }

            } catch (Exception e) {
                log.error("Generation error: {}", e.getMessage());
                /* Возвращаем генерацию при исключении*/
                balanceService.addImageGenerations(user.getId(), 1);
                telegramService.sendMessage(chatId,
                        "❌ Ошибка при генерации\n" +
                                "🎨 Баланс возвращен"
                );
            } finally {
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            }
        });
    }

    public void handleVideoGeneration(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Проверяем баланс видео*/
        if (balanceService.getVideoBalance(user.getId()) <= 0) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций видео!\n\n" +
                            "🎥 Баланс: 0 видео\n" +
                            "🛒 Купите пакет видео в магазине"
            );
            return;
        }

        telegramService.sendMessage(chatId,
                "🎥 Генерация видео пока в разработке...\n\n" +
                        "📝 Промпт: _" + prompt + "_\n\n" +
                        "⚠️ Функция появится в ближайшее время"
        );

        /* TODO: Добавить списание видео-баланса когда реализуем*/
        /* boolean used = balanceService.useVideoGeneration(user.getId());*/
    }

}
