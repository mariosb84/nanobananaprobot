package org.example.nanobananaprobot.bot.handlers;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface PaymentHandler {
    void handlePackagePurchase(Long chatId, String packageType, String count);
    void handlePaymentCheck(Long chatId, String paymentId);
    void handlePaymentCheckCallback(CallbackQuery callbackQuery, String paymentId);
    /* УБИРАЕМ старые методы: handleSubscriptionPayment, checkAutoPayment*/
}
