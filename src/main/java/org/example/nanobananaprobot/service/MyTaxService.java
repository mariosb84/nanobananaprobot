package org.example.nanobananaprobot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.loolzaaa.nalog.mytax.client.MyTaxClient;
import ru.loolzaaa.nalog.mytax.client.MyTaxClientConfig;
import ru.loolzaaa.nalog.mytax.client.pojo.Receipt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class MyTaxService {

    @Value("${mytax.inn}")
    private String inn;

    @Value("${mytax.password}")
    private String password;

    private volatile String cookies;
    private volatile LocalDateTime cookiesFetchedAt;

    private static final String NALOG_URL = "https://lknpd.nalog.ru";
    private static final String PROXY_HOST = "94.141.162.141";
    private static final int PROXY_PORT = 8888;

    @PostConstruct
    public void init() {
        fetchCookies();
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // каждые 24 часа
    public void scheduledCookiesRefresh() {
        log.info("Обновление кук по расписанию");
        fetchCookies();
    }

    private synchronized void fetchCookies() {

        /* Генерируем любые куки — налоговая проверяет только наличие заголовка Cookie*/

        cookies = "_ym_uid=0000000000000000; _ym_d=0; _ym_isad=2";
        cookiesFetchedAt = LocalDateTime.now();
        log.info("Куки установлены: {}", cookies);
    }

    /*private synchronized void fetchCookies() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .proxy(java.net.ProxySelector.of(
                            new java.net.InetSocketAddress(PROXY_HOST, PROXY_PORT)))
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NALOG_URL))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");
            if (setCookieHeaders.isEmpty()) {
                log.warn("Куки не получены от {}", NALOG_URL);
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (String header : setCookieHeaders) {
                String cookieValue = header.split(";")[0];
                sb.append(cookieValue).append("; ");
            }
            cookies = sb.toString().replaceAll("; $", "");
            cookiesFetchedAt = LocalDateTime.now();
            log.info("Куки обновлены: {}", cookies);

        } catch (Exception e) {
            log.error("Ошибка получения кук: {}", e.getMessage());
        }
    }*/

    private String getCookies() {
        if (cookies == null || cookiesFetchedAt == null ||
                ChronoUnit.HOURS.between(cookiesFetchedAt, LocalDateTime.now()) >= 24) {
            fetchCookies();
        }
        return cookies;
    }

    public void sendReceipt(String description, double amount) {
        if (inn == null || inn.isBlank() || password == null || password.isBlank()) {
            log.warn("⚠️ Не настроены ИНН или пароль для отправки чеков");
            return;
        }

        try {
            String currentCookies = getCookies();

            MyTaxClientConfig config = new MyTaxClientConfig();
            config.setPrefix("NanoBananaBot");
            config.setZoneOffset("+3");
            config.setProxyHost(PROXY_HOST);
            config.setProxyPort(PROXY_PORT);
            config.setCookies(currentCookies); /* нужно добавить это поле в форк*/

            MyTaxClient client = new MyTaxClient(config);
            client.init(inn, password);
            log.info("✅ Авторизация в Мой налог успешна");

            ru.loolzaaa.nalog.mytax.client.pojo.Service service =
                    new ru.loolzaaa.nalog.mytax.client.pojo.Service(description, 1, amount);

            Receipt receipt = client.addIncome(List.of(service));
            log.info("✅ Чек отправлен! Сумма: {} руб., Ссылка: {}", amount, receipt.printUrl());

        } catch (Exception e) {
            log.error("❌ Ошибка отправки чека: {}", e.getMessage(), e);

            /* Если ошибка авторизации — пробуем обновить куки и повторить*/

            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("403") ||
                    e.getMessage().contains("authentication") || e.getMessage().contains("Unauthorized"))) {
                log.info("Попытка обновить куки после ошибки авторизации");
                fetchCookies();
            }
        }
    }
}

