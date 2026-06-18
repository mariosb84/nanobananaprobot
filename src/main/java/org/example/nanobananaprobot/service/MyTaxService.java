package org.example.nanobananaprobot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.loolzaaa.nalog.mytax.client.MyTaxClient;
import ru.loolzaaa.nalog.mytax.client.MyTaxClientConfig;
import ru.loolzaaa.nalog.mytax.client.pojo.Receipt;

import java.util.List;

@Slf4j
@Service
public class MyTaxService {

    @Value("${mytax.inn}")
    private String inn;

    @Value("${mytax.password}")
    private String password;

    public void sendReceipt(String description, double amount) {
        if (inn == null || inn.isBlank() || password == null || password.isBlank()) {
            log.warn("⚠️ Не настроены ИНН или пароль для отправки чеков");
            return;
        }

        try {

            /* Конфигурация клиента*/

            MyTaxClientConfig config = new MyTaxClientConfig();
            config.setPrefix("NanoBananaBot");
            config.setZoneOffset("+3");

            /* Настройка прокси */
            config.setProxyHost("94.141.162.141");
            config.setProxyPort(8888);

            /* Создаём клиента*/

            MyTaxClient client = new MyTaxClient(config);
            client.init(inn, password);
            log.info("✅ Авторизация в Мой налог успешна");

            /* Используем полное имя класса (без импорта)*/

            ru.loolzaaa.nalog.mytax.client.pojo.Service service =
                    new ru.loolzaaa.nalog.mytax.client.pojo.Service(description, 1, amount);

            Receipt receipt = client.addIncome(List.of(service));
            log.info("✅ Чек отправлен! Сумма: {} руб., Ссылка: {}", amount, receipt.printUrl());

        } catch (Exception e) {
            log.error("❌ Ошибка отправки чека: {}", e.getMessage(), e);
        }
    }

}