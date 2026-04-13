package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.bot.service.UserStateManager;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackHandlerImpl implements CallbackHandler {

    private final TelegramService telegramService;
    private final PaymentHandler paymentHandler;
    private final UserStateManager stateManager;
    private final MessageHandler messageHandler;

    private void handlePaymentCheckCallback(CallbackQuery callbackQuery, String data) {
        /* Сразу отвечаем на колбэк*/
        answerCallback(callbackQuery, "⏳ Проверяем платеж...");

        try {
            String paymentId = data.substring("check_payment_".length());
            paymentHandler.handlePaymentCheckCallback(callbackQuery, paymentId);
        } catch (Exception e) {
            log.error("Error handling payment callback: {}", e.getMessage());
            telegramService.sendMessage(callbackQuery.getMessage().getChatId(), "❌ Ошибка проверки платежа");
        }
    }

    private void answerCallback(CallbackQuery callbackQuery, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText(text);
            answer.setShowAlert(false); /* Всплывающее уведомление*/
            telegramService.answerCallback(answer);
        } catch (Exception e) {
            log.warn("Failed to answer callback: {}", e.getMessage());
        }
    }

    @Override
    public void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        log.debug("Handling callback - ChatId: {}, Data: {}", chatId, data);

        try {
            if (data.startsWith("check_payment_")) {
                handlePaymentCheckCallback(callbackQuery, data);
            } else if (data.equals("edit_photo") || data.equals("add_more_photo") ||
                    data.equals("merge_continue") || data.equals("cancel_photo")) {
                handlePhotoActions(callbackQuery);
            } else if ("start_generation".equals(data)) {  /* ← добавить этот блок*/
                handleStartGeneration(callbackQuery);
            } else {
                answerCallback(callbackQuery, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage());
        }
    }

    private void handleStartGeneration(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        String textToProceed = "Отправьте пожалуйста фото\n" +
                "с описанием, или альбом из\n" +
                "фотографий с описанием\n" +
                "чтобы приступить к генерации 👇\n\n" +
                "Либо просто введите промпт\n" +
                "для генерации.👇\n\n" +
                "Важно - отправьте фото и описание\n" +
                "одним сообщением!";

        telegramService.sendMessage(chatId, textToProceed);
        //stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PROMPT);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_USER_INPUT);
        answerCallback(callbackQuery, "✅ Начинаем генерацию");
    }

   /* private void handlePhotoActions(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        switch (data) {
            *//*case "edit_photo" -> {
                // Режим редактирования
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EDIT_PROMPT);
                telegramService.sendMessage(chatId, "✏️ Введите описание изменений для фото:");
                answerCallback(callbackQuery, "✅ Режим редактирования");
            }*//*
            case "edit_photo" -> {
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_SETTINGS);
                messageHandler.showSettingsMenu(chatId);  // ← показать настройки
                answerCallback(callbackQuery, "✅ Выберите настройки");
            }
            case "add_more_photo" -> {
                // Добавить ещё фото для слияния
                byte[] firstPhoto = stateManager.getUploadedImage(chatId);
                stateManager.clearUploadedImage(chatId);
                stateManager.addImageToCollection(chatId, firstPhoto, null);
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EXTRA_PHOTO);
                telegramService.sendMessage(chatId, "📸 Отправьте следующее фото.\n\nПосле загрузки 2+ фото появится кнопка 'Продолжить'");
                answerCallback(callbackQuery, "➕ Жду следующее фото");
            }
            case "cancel_photo" -> {
                stateManager.clearUploadedImage(chatId);
                stateManager.clearMultipleImages(chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                messageHandler.showMainMenuCompact(chatId);
                answerCallback(callbackQuery, "❌ Действие отменено");
            }
           *//* case "merge_continue" -> {
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MERGE_PROMPT);
                telegramService.sendMessage(chatId, "📝 Введите описание для слияния фото:");
                answerCallback(callbackQuery, "✅ Переход к промпту");
            }*//*
            case "merge_continue" -> {
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_SETTINGS);
                messageHandler.showSettingsMenu(chatId);  // ← показать настройки
                answerCallback(callbackQuery, "✅ Выберите настройки для слияния");
            }
        }
    }*/

    private void handlePhotoActions(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        switch (data) {

            case "edit_photo" -> {
                // Переходим в режим ожидания промпта для редактирования
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EDIT_PROMPT);
                telegramService.sendMessage(chatId, "✏️ Введите описание изменений для фото:");
                answerCallback(callbackQuery, "✅ Режим редактирования");
            }

           /* case "edit_photo" -> {
                // Переходим в режим настроек для редактирования
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_SETTINGS);
                messageHandler.showSettingsMenu(chatId);
                answerCallback(callbackQuery, "✅ Выберите настройки для редактирования");
            }*/
            case "add_more_photo" -> {
                // Переключаемся в режим сбора нескольких фото
                byte[] firstPhoto = stateManager.getUploadedImage(chatId);
                stateManager.clearUploadedImage(chatId);
                if (firstPhoto != null) {
                    stateManager.addImageToCollection(chatId, firstPhoto, null);
                }
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD);
                telegramService.sendMessage(chatId, "📸 Отправьте следующее фото (нужно минимум 2)");
                answerCallback(callbackQuery, "➕ Жду следующее фото");
            }
            case "merge_continue" -> {
                // Переходим в режим настроек для слияния
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_SETTINGS);
                messageHandler.showSettingsMenu(chatId);
                answerCallback(callbackQuery, "✅ Выберите настройки для слияния");
            }
            case "cancel_photo" -> {
                stateManager.clearUploadedImage(chatId);
                stateManager.clearMultipleImages(chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                messageHandler.showMainMenuCompact(chatId);
                answerCallback(callbackQuery, "❌ Действие отменено");
            }
        }
    }

}
