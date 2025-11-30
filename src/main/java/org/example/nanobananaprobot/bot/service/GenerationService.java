package org.example.nanobananaprobot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.HiggsfieldAIService;
import org.example.nanobananaprobot.service.SubscriptionService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final HiggsfieldAIService higgsfieldService;
    private final UserServiceData userService;
    private final SubscriptionService subscriptionService;
    private final TelegramService telegramService;
    private final UserStateManager stateManager;

    public void handleImageGeneration(Long chatId, String prompt) {
        if (!checkSubscription(chatId)) return;

        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId, "🎨 Генерирую изображение...\n\nПромпт: _" + prompt + "_");

        CompletableFuture.runAsync(() -> {
            try {
                var response = higgsfieldService.generateImage(prompt);

                if ("success".equals(response.getStatus()) &&
                        response.getImages() != null &&
                        !response.getImages().isEmpty()) {

                    /* Отправляем результат*/
                    String imageUrl = response.getImages().get(0);
                    telegramService.sendMessage(chatId,
                            "✅ Изображение готово!\n\n" +
                                    "🔗 Ссылка: " + imageUrl + "\n" +
                                    "📝 Промпт: _" + prompt + "_"
                    );

                } else {
                    telegramService.sendMessage(chatId,
                            "❌ Ошибка генерации: " + response.getError()
                    );
                }

            } catch (Exception e) {
                log.error("Generation error: {}", e.getMessage());
                telegramService.sendMessage(chatId, "❌ Ошибка при генерации");
            } finally {
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            }
        });
    }

    public void handleVideoGeneration(Long chatId, String prompt) {
        if (!checkSubscription(chatId)) return;

        telegramService.sendMessage(chatId, "🎥 Генерация видео пока в разработке...");
        /* TODO: Реализовать когда добавим видео API*/
    }

    private boolean checkSubscription(Long chatId) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null || !subscriptionService.isSubscriptionActive(user.getUsername())) {
            telegramService.sendMessage(chatId, "❌ Требуется активная подписка!");
            return false;
        }
        return true;
    }

}