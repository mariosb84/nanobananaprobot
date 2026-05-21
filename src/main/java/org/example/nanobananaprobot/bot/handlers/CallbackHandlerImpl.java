package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.bot.service.UserStateManager;
import org.example.nanobananaprobot.domain.dto.ImageConfig;
import org.example.nanobananaprobot.domain.dto.TokenConfig;
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

    private final TokenConfig tokenConfig;

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
            } else if ("start_generation".equals(data)) {
                handleStartGeneration(callbackQuery);
            }
            /* Покупка токенов*/
            else if (data.startsWith("token_")) {
                handleTokenPurchaseCallback(callbackQuery);
            }
            else if (data.equals("back_to_menu")) {
                messageHandler.showMainMenuCompact(chatId);
                answerCallback(callbackQuery, "🏠 Главное меню");
            }
            /* Настройки */
            else if (data.equals("settings_format")) {
                messageHandler.showAspectRatioSelection(chatId);
                answerCallback(callbackQuery, "✅ Выберите формат");
            }
            else if (data.equals("settings_quality")) {
                messageHandler.showResolutionSelection(chatId);
                answerCallback(callbackQuery, "✅ Выберите качество");
            }
            else if (data.equals("settings_generate")) {
                messageHandler.startGenerationWithCurrentSettings(chatId);
                answerCallback(callbackQuery, "✅ Начинаем генерацию");
            }
            else if (data.equals("settings_cancel")) {
                stateManager.clearUserData(chatId);
                messageHandler.showMainMenuCompact(chatId);
                answerCallback(callbackQuery, "❌ Отменено");
            }
            /* Выбор формата */
            else if (data.equals("ratio_1_1")) {
                updateAspectRatio(chatId, "1:1", callbackQuery);
            }
            else if (data.equals("ratio_16_9")) {
                updateAspectRatio(chatId, "16:9", callbackQuery);
            }
            else if (data.equals("ratio_21_9")) {
                updateAspectRatio(chatId, "21:9", callbackQuery);
            }
            else if (data.equals("ratio_4_3")) {
                updateAspectRatio(chatId, "4:3", callbackQuery);
            }
            else if (data.equals("ratio_9_16")) {
                updateAspectRatio(chatId, "9:16", callbackQuery);
            }
            else if (data.equals("ratio_2_3")) {
                updateAspectRatio(chatId, "2:3", callbackQuery);
            }
            else if (data.equals("ratio_3_2")) {
                updateAspectRatio(chatId, "3:2", callbackQuery);
            }
            else if (data.equals("ratio_3_4")) {
                updateAspectRatio(chatId, "3:4", callbackQuery);
            }
            else if (data.equals("ratio_4_5")) {
                updateAspectRatio(chatId, "4:5", callbackQuery);
            }
            else if (data.equals("ratio_5_4")) {
                updateAspectRatio(chatId, "5:4", callbackQuery);
            }
            /* Выбор качества */
            else if (data.equals("res_1K")) {
                updateResolution(chatId, "1K", callbackQuery);
            }
            else if (data.equals("res_2K")) {
                updateResolution(chatId, "2K", callbackQuery);
            }
            else if (data.equals("res_4K")) {
                updateResolution(chatId, "4K", callbackQuery);
            }
            /* Назад к настройкам */
            else if (data.equals("back_to_settings")) {
                messageHandler.showSettingsMenu(chatId);
                answerCallback(callbackQuery, "⚙️ Настройки");
            }
            else {
                answerCallback(callbackQuery, "❌ Неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage());
        }
    }

   /* private void handleTokenPurchaseCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        int tokens;
        int price;

        switch (data) {
            case "token_5" -> { tokens = 5; price = 25; }
            case "token_10" -> { tokens = 10; price = 50; }
            case "token_30" -> { tokens = 30; price = 150; }
            case "token_50" -> { tokens = 50; price = 250; }
            case "token_100" -> { tokens = 100; price = 500; }
            default -> {
                answerCallback(callbackQuery, "❌ Неизвестный пакет");
                return;
            }
        }

        paymentHandler.handleTokenPackagePurchase(chatId, String.valueOf(tokens), String.valueOf(price));
        answerCallback(callbackQuery, "💳 Оформляем покупку...");
        telegramService.sendMessage(chatId, "💳 *Оплата пакета токенов*\n\n" +
                "💰 Пакет: " + tokens + " токенов\n" +
                "💵 Сумма: " + price + " ₽\n\n" +
                "Ссылка для оплаты будет сформирована...", "Markdown");
    }*/

    private void handleTokenPurchaseCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        int tokens;
        int price;

        int pricePerToken = tokenConfig.getPriceRub(); /* получаем цену 1 токена*/

        switch (data) {
            case "token_5" -> {
                tokens = 5;
                price = tokens * pricePerToken;
            }
            case "token_10" -> {
                tokens = 10;
                price = tokens * pricePerToken;
            }
            case "token_30" -> {
                tokens = 30;
                price = tokens * pricePerToken;
            }
            case "token_50" -> {
                tokens = 50;
                price = tokens * pricePerToken;
            }
            case "token_100" -> {
                tokens = 100;
                price = tokens * pricePerToken;
            }
            default -> {
                answerCallback(callbackQuery, "❌ Неизвестный пакет");
                return;
            }
        }

        paymentHandler.handleTokenPackagePurchase(chatId, String.valueOf(tokens), String.valueOf(price));
        answerCallback(callbackQuery, "💳 Оформляем покупку...");
        telegramService.sendMessage(chatId, "💳 *Оплата пакета токенов*\n\n" +
                "💰 Пакет: " + tokens + " токенов\n" +
                "💵 Сумма: " + price + " ₽\n\n" +
                "Ссылка для оплаты будет сформирована...", "Markdown");
    }

    private void handleStartGeneration(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        stateManager.clearMultipleImages(chatId);
        stateManager.clearUploadedImage(chatId);
        stateManager.clearTempData(chatId);

        String textToProceed = "Отправьте пожалуйста фото,\n" +
                "или альбом из\n" +
                "фотографий\n" +
                "чтобы приступить к генерации 👇\n\n" +
                "Либо просто введите промпт\n" +
                "для генерации.👇\n"
               ;

        telegramService.sendMessage(chatId, textToProceed);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_USER_INPUT);
        answerCallback(callbackQuery, "✅ Начинаем генерацию");
    }



    private void handlePhotoActions(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        switch (data) {

            case "edit_photo" -> {
                /* Переходим в режим ожидания промпта для редактирования*/
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_EDIT_PROMPT);
                telegramService.sendMessage(chatId, "✏️ Введите описание изменений для фото:");
                answerCallback(callbackQuery, "✅ Режим редактирования");
            }
            case "add_more_photo" -> {

                log.info("=== НАЧАЛО add_more_photo ===");
                log.info("До очистки: multipleImages size = {}",
                        stateManager.getMultipleImages(chatId) != null ? stateManager.getMultipleImages(chatId).size() : 0);

                byte[] firstPhoto = stateManager.getUploadedImage(chatId);

                /* Очищаем старые данные перед новым слиянием*/
                stateManager.clearMultipleImages(chatId);
                stateManager.clearUploadedImage(chatId);

                log.info("После очистки: multipleImages size = {}",
                        stateManager.getMultipleImages(chatId) != null ? stateManager.getMultipleImages(chatId).size() : 0);

                /* Переключаемся в режим сбора нескольких фото*/

                stateManager.clearUploadedImage(chatId);
                if (firstPhoto != null) {
                    stateManager.addImageToCollection(chatId, firstPhoto, null);

                    log.info("Первое фото добавлено, size = {}",
                            stateManager.getMultipleImages(chatId).size());
                }
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD);
                telegramService.sendMessage(chatId, "📸 Отправьте следующее фото (нужно минимум 2)");
                answerCallback(callbackQuery, "➕ Жду следующее фото");
            }
            case "merge_continue" -> {
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MERGE_PROMPT);
                telegramService.sendMessage(chatId, "📝 Введите описание для слияния фото:");
                answerCallback(callbackQuery, "✅ Теперь введите промпт");
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

    private void updateAspectRatio(Long chatId, String ratio, CallbackQuery callbackQuery) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        config.setAspectRatio(ratio);
        stateManager.saveConfig(chatId, config);

        messageHandler.showSettingsMenu(chatId);
        answerCallback(callbackQuery, "✅ Формат изменён на " + ratio);
    }

    private void updateResolution(Long chatId, String resolution, CallbackQuery callbackQuery) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        config.setResolution(resolution);
        stateManager.saveConfig(chatId, config);

        messageHandler.showSettingsMenu(chatId);
        answerCallback(callbackQuery, "✅ Качество изменено на " + resolution);
    }

}
