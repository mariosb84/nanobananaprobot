package org.example.nanobananaprobot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.domain.dto.PaymentCreateRequest;
import org.example.nanobananaprobot.domain.dto.PaymentCreateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final YooKassaClient yooKassaClient;
    private final PaymentAutoCheckService packageAutoCheckService;
    private final UserServiceData userService;

    @Value("${amountSetCurrency}")
    private String amountSetCurrency;

    /* СТАРЫЙ МЕТОД ДЛЯ ПОДПИСОК - МОЖНО УДАЛИТЬ ИЛИ ОСТАВИТЬ КАК ЗАГЛУШКУ*/
    public PaymentCreateResponse createPayment(Long chatId, String price, String description) {
        try {
            PaymentCreateRequest request = new PaymentCreateRequest();

            PaymentCreateRequest.Amount amount = new PaymentCreateRequest.Amount();
            amount.setValue(price);
            amount.setCurrency(this.amountSetCurrency);

            request.setAmount(amount);
            request.setDescription(description);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("chatId", chatId.toString());
            metadata.put("itemType", "CUSTOM");

            var user = userService.findByTelegramChatId(chatId);
            if (user != null) {
                metadata.put("userId", user.getUsername());
            }
            request.setMetadata(metadata);

            /* Чек для 54-ФЗ*/
            PaymentCreateRequest.Receipt receipt = new PaymentCreateRequest.Receipt();
            PaymentCreateRequest.Customer customer = new PaymentCreateRequest.Customer();
            if (user != null) {
                customer.setEmail(user.getEmail());
            }
            receipt.setCustomer(customer);

            PaymentCreateRequest.Item item = new PaymentCreateRequest.Item();
            item.setDescription(description);
            item.setAmount(amount);

            List<PaymentCreateRequest.Item> items = new ArrayList<>();
            items.add(item);
            receipt.setItems(items);
            request.setReceipt(receipt);

            PaymentCreateRequest.Confirmation confirmation = new PaymentCreateRequest.Confirmation();
            confirmation.setType("embedded");
            request.setConfirmation(confirmation);

            PaymentCreateResponse response = yooKassaClient.createPayment(request);
            log.info("Created payment - ChatId: {}, Description: {}, Price: {}, PaymentId: {}",
                    chatId, description, price, response.getId());

            return response;

        } catch (Exception e) {
            log.error("Failed to create payment for chatId: {}", chatId, e);
            throw new RuntimeException("Payment creation failed", e);
        }
    }

    /* ОСНОВНОЙ МЕТОД ДЛЯ ПАКЕТОВ*/
    public PaymentCreateResponse createPackagePayment(Long chatId, String price, String description,
                                                      String packageType, String count) {
        try {
            PaymentCreateRequest request = new PaymentCreateRequest();

            PaymentCreateRequest.Amount amount = new PaymentCreateRequest.Amount();
            amount.setValue(price);
            amount.setCurrency(this.amountSetCurrency);

            request.setAmount(amount);
            request.setDescription(description);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("chatId", chatId.toString());
            metadata.put("packageType", packageType);
            metadata.put("count", count);
            metadata.put("price", price);
            metadata.put("itemType", "PACKAGE");

            var user = userService.findByTelegramChatId(chatId);
            if (user != null) {
                metadata.put("userId", user.getUsername());
            }
            request.setMetadata(metadata);

            PaymentCreateRequest.Receipt receipt = new PaymentCreateRequest.Receipt();
            PaymentCreateRequest.Customer customer = new PaymentCreateRequest.Customer();
            if (user != null) {
                customer.setEmail(user.getEmail());
            }
            receipt.setCustomer(customer);

            PaymentCreateRequest.Item item = new PaymentCreateRequest.Item();
            item.setDescription(description);
            item.setAmount(amount);

            List<PaymentCreateRequest.Item> items = new ArrayList<>();
            items.add(item);
            receipt.setItems(items);
            request.setReceipt(receipt);

            PaymentCreateRequest.Confirmation confirmation = new PaymentCreateRequest.Confirmation();
            confirmation.setType("embedded");
            request.setConfirmation(confirmation);

            PaymentCreateResponse response = yooKassaClient.createPayment(request);
            log.info("Created package payment - ChatId: {}, Type: {}, Count: {}, Price: {}, PaymentId: {}",
                    chatId, packageType, count, price, response.getId());

            packageAutoCheckService.startPackageCheck(
                    response.getId(),
                    chatId,
                    packageType,
                    count,
                    price
            );

            return response;

        } catch (Exception e) {
            log.error("Failed to create package payment for chatId: {}", chatId, e);
            throw new RuntimeException("Package payment creation failed", e);
        }
    }

    public PaymentCreateResponse getPaymentStatus(String paymentId) {
        return yooKassaClient.getPayment(paymentId);
    }

    /* МЕТОД ДЛЯ ОБРАБОТКИ ВЕБХУКОВ (ЕСЛИ БУДУТ ИСПОЛЬЗОВАТЬСЯ)*/
    public void handleWebhook(Object webhook) {
        log.info("Received webhook: {}", webhook);
        /* TODO: Реализовать при необходимости*/
    }

}
