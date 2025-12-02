package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.keyboards.MenuFactory;
import org.example.nanobananaprobot.bot.service.PackageService;
import org.example.nanobananaprobot.bot.service.PaymentInfo;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.domain.dto.PaymentCreateResponse;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.PaymentAutoCheckService;
import org.example.nanobananaprobot.service.PaymentService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHandlerImpl implements PaymentHandler {

    @Value("${paymentUrl}")
    private String paymentUrl;

    private final GenerationBalanceService balanceService;
    private final PackageService packageService;
    private final PaymentService paymentService;
    private final UserServiceData userService;
    private final MenuFactory menuFactory;
    private final TelegramService telegramService;
    private final PaymentAutoCheckService packageAutoCheckService;

    private final Map<String, PaymentInfo> pendingPayments = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public void handlePackagePurchase(Long chatId, String packageType, String count) {
        executor.submit(() -> {
            try {
                User user = userService.findByTelegramChatId(chatId);
                if (user == null) {
                    telegramService.sendMessage(chatId, "❌ Пользователь не найден");
                    return;
                }

                String price;
                String description;

                if ("image".equals(packageType)) {
                    price = packageService.getImagePackagePrice(count);
                    description = "Пакет " + count + " генераций изображений";
                } else {
                    price = packageService.getVideoPackagePrice(count);
                    description = "Пакет " + count + " генераций видео";
                }

                var paymentResponse = paymentService.createPackagePayment(
                        chatId,
                        price,
                        description,
                        packageType,
                        count
                );

                if (paymentResponse != null && paymentResponse.getId() != null) {
                    savePaymentInfo(chatId, paymentResponse.getId(), packageType, count, price);

                    /* Запускаем автоматическую проверку*/
                    packageAutoCheckService.startPackageCheck(
                            paymentResponse.getId(),
                            chatId,
                            packageType,
                            count,
                            price
                    );

                    String confirmationUrl = getConfirmationUrl(paymentResponse);
                    String paymentUrl = confirmationUrl != null ? confirmationUrl :
                            this.paymentUrl + paymentResponse.getId();

                    String messageText = "💳 *Оплата пакета генераций*\n\n" +
                            "📦 Пакет: " + description + "\n" +
                            "💰 Сумма: " + price + " ₽\n\n" +
                            "🔗 Ссылка для оплаты:\n" +
                            paymentUrl + "\n\n" +
                            "После успешной оплаты генерации добавятся автоматически!";

                    telegramService.sendMessage(chatId, messageText);

                } else {
                    telegramService.sendMessage(chatId, "❌ Ошибка создания платежа");
                }

            } catch (Exception e) {
                log.error("Package purchase error for chatId: {}", chatId, e);
                telegramService.sendMessage(chatId, "❌ Ошибка при создании платежа");
            }
        });
    }

    @Override
    public void handlePaymentCheck(Long chatId, String paymentId) {
        executor.submit(() -> {
            try {
                var payment = paymentService.getPaymentStatus(paymentId);

                if (payment != null && "succeeded".equals(payment.getStatus())) {
                    PaymentInfo paymentInfo = pendingPayments.get(paymentId);

                    if (paymentInfo != null) {
                        User user = userService.findByTelegramChatId(chatId);

                        if (user != null) {
                            if ("image".equals(paymentInfo.getPackageType())) {
                                balanceService.addImageGenerations(user.getId(),
                                        Integer.parseInt(paymentInfo.getCount()));
                                telegramService.sendMessage(chatId,
                                        "✅ Пакет из " + paymentInfo.getCount() +
                                                " генераций изображений добавлен!\n" +
                                                "🎨 Новый баланс: " + balanceService.getImageBalance(user.getId()));
                            } else {
                                balanceService.addVideoGenerations(user.getId(),
                                        Integer.parseInt(paymentInfo.getCount()));
                                telegramService.sendMessage(chatId,
                                        "✅ Пакет из " + paymentInfo.getCount() +
                                                " генераций видео добавлен!\n" +
                                                "🎥 Новый баланс: " + balanceService.getVideoBalance(user.getId()));
                            }

                            pendingPayments.remove(paymentId);
                        }
                    } else {
                        telegramService.sendMessage(chatId, "⏳ Проверяем информацию о платеже...");
                    }
                } else {
                    telegramService.sendMessage(chatId, "❌ Платеж не найден или не завершен");
                }
            } catch (Exception e) {
                log.error("Error checking payment: {}", e.getMessage());
                telegramService.sendMessage(chatId, "❌ Ошибка проверки платежа");
            }
        });
    }

    @Override
    public void handlePaymentCheckCallback(CallbackQuery callbackQuery, String paymentId) {
        executor.submit(() -> {
            try {
                var paymentStatus = paymentService.getPaymentStatus(paymentId);
                Long chatId = callbackQuery.getMessage().getChatId();

                if ("succeeded".equals(paymentStatus.getStatus())) {
                    answerCallback(callbackQuery, "✅ Платеж успешно завершен!");
                    telegramService.sendMessage(chatId, "🎉 Платеж подтвержден! Генерации добавлены.");
                } else if ("pending".equals(paymentStatus.getStatus())) {
                    answerCallback(callbackQuery, "⏳ Платеж еще обрабатывается...");
                } else {
                    answerCallback(callbackQuery, "❌ Платеж не прошел");
                }
            } catch (Exception e) {
                log.error("Error checking payment callback: {}", e.getMessage());
                answerCallback(callbackQuery, "❌ Ошибка проверки статуса");
            }
        });
    }

    private void savePaymentInfo(Long chatId, String paymentId, String packageType, String count, String price) {
        PaymentInfo info = new PaymentInfo(paymentId, packageType, count, price, chatId);
        pendingPayments.put(paymentId, info);
    }

    private String getConfirmationUrl(PaymentCreateResponse response) {
        if (response.getConfirmation() != null && response.getConfirmation().getConfirmationUrl() != null) {
            return response.getConfirmation().getConfirmationUrl();
        }
        return null;
    }

    private void answerCallback(CallbackQuery callbackQuery, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText(text);
        telegramService.answerCallback(answer);
    }

}

