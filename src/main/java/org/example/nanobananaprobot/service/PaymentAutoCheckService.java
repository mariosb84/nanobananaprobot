package org.example.nanobananaprobot.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.service.PaymentInfo;
import org.example.nanobananaprobot.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAutoCheckService {

    private final YooKassaClient yooKassaClient;
    private final GenerationBalanceService balanceService;
    private final UserServiceData userService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> activeChecks = new ConcurrentHashMap<>();
    private final Map<String, PaymentInfo> packagePayments = new ConcurrentHashMap<>();

    public void startPackageCheck(String paymentId, Long chatId, String packageType, String count, String price) {
        log.info("Starting package check - Payment: {}, Chat: {}, Type: {}, Count: {}",
                paymentId, chatId, packageType, count);

        /* Сохраняем информацию о пакете*/

        PaymentInfo paymentInfo = new PaymentInfo(paymentId, packageType, count, price, chatId);
        packagePayments.put(paymentId, paymentInfo);

        /* Запускаем проверку каждые 30 секунд*/

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                checkPackagePayment(paymentId);
            } catch (Exception e) {
                log.error("Error checking package payment: {}", paymentId, e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        activeChecks.put(paymentId, task);

        /* Останавливаем через 2 часа (максимальное время ожидания)*/

        scheduler.schedule(() -> {
            if (activeChecks.containsKey(paymentId)) {
                log.info("Package check expired for payment: {}", paymentId);
                stopPackageCheck(paymentId);
            }
        }, 2, TimeUnit.HOURS);
    }

    private void checkPackagePayment(String paymentId) {
        try {
            var paymentStatus = yooKassaClient.getPayment(paymentId);
            log.debug("Package check - Payment: {}, Status: {}", paymentId, paymentStatus.getStatus());

            if ("succeeded".equals(paymentStatus.getStatus())) {
                activatePackage(paymentId);
                stopPackageCheck(paymentId);
            } else if ("canceled".equals(paymentStatus.getStatus())) {
                log.info("Package payment canceled: {}", paymentId);
                stopPackageCheck(paymentId);
            }

        } catch (Exception e) {
            log.error("Error checking package payment status: {}", paymentId, e);
        }
    }

    private void activatePackage(String paymentId) {
        try {
            PaymentInfo paymentInfo = packagePayments.get(paymentId);
            if (paymentInfo == null) {
                log.error("Payment info not found for: {}", paymentId);
                return;
            }

            Long chatId = paymentInfo.getChatId();
            User user = userService.findByTelegramChatId(chatId);
            if (user == null) {
                log.error("User not found for chatId: {}", chatId);
                return;
            }

            /* Активируем пакет*/

            if ("image".equals(paymentInfo.getPackageType())) {
                int count = Integer.parseInt(paymentInfo.getCount());
                balanceService.addImageGenerations(user.getId(), count);
                log.info("Image package activated - User: {}, Count: {}, Payment: {}",
                        user.getUsername(), count, paymentId);
            } else if ("video".equals(paymentInfo.getPackageType())) {
                int count = Integer.parseInt(paymentInfo.getCount());
                balanceService.addVideoGenerations(user.getId(), count);
                log.info("Video package activated - User: {}, Count: {}, Payment: {}",
                        user.getUsername(), count, paymentId);
            }

        } catch (Exception e) {
            log.error("Error activating package: {}", paymentId, e);
        } finally {
            packagePayments.remove(paymentId);
        }
    }

    public void stopPackageCheck(String paymentId) {
        ScheduledFuture<?> task = activeChecks.get(paymentId);
        if (task != null) {
            task.cancel(false);
            activeChecks.remove(paymentId);
            packagePayments.remove(paymentId);
            log.info("Package check stopped for payment: {}", paymentId);
        }
    }

    public boolean isPackageCheckActive(String paymentId) {
        return activeChecks.containsKey(paymentId);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down PackageAutoCheckService");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
