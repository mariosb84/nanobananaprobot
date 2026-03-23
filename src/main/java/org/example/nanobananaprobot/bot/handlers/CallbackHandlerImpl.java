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
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PROMPT);
        answerCallback(callbackQuery, "✅ Начинаем генерацию");
    }

}
