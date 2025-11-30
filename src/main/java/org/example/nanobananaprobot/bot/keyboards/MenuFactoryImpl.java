package org.example.nanobananaprobot.bot.keyboards;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.constants.TextConstants;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.SubscriptionService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuFactoryImpl implements MenuFactory {

    @Value("${app.subscription.monthly.price}")
    private String monthlyPrice;

    @Value("${app.subscription.yearly.price}")
    private String yearlyPrice;

    @Value("${currencySecond}")
    private String currencySecond;

    private final UserServiceData userService;
    private final SubscriptionService subscriptionService;

    @Override
    public SendMessage createWelcomeMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(TextConstants.WELCOME_TEXT.getText());
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📝 Зарегистрироваться"));
        row1.add(new KeyboardButton("🔑 Войти"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📋 Информация"));
        row2.add(new KeyboardButton("📞 Контакты"));

        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Override
    public SendMessage createMainMenu(Long chatId) {
        return createMainMenu(chatId, false);
    }

    @Override
    public SendMessage createMainMenu(Long chatId, boolean afterGeneration) {
        User user = userService.findByTelegramChatId(chatId);
        String status = user != null ? getSubscriptionStatus(user.getUsername()) : "❌ Подписка: не активна";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        if (!afterGeneration) {
            message.setText("🏠 *Главное меню*\n\n" + status + "\n\nВыберите действие:");
        } else {
            message.setText("✅ *Генерация завершена!*\n\n" + status + "\n\nВыберите следующее действие:");
        }
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Основные функции генерации
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎨 Сгенерировать изображение"));
        row1.add(new KeyboardButton("🎥 Сгенерировать видео"));

        // Управление подпиской
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💳 Оплатить подписку"));
        row2.add(new KeyboardButton("📊 Статистика"));

        // Информация и настройки
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📋 Информация"));
        row3.add(new KeyboardButton("📞 Контакты"));

        // Выход
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("❌ Выйти"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Override
    public SendMessage createSubscriptionMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("💳 *Выбор подписки*\n\n" +
                "✅ Генерация изображений\n" +
                "✅ Создание видео\n" +
                "✅ Все модели AI\n" +
                "✅ Приоритетная очередь\n\n" +
                "*После оплаты подписка активируется автоматически!*");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("1 месяц - " + this.monthlyPrice + this.currencySecond));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("12 месяцев - " + this.yearlyPrice + this.currencySecond));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    // УДАЛЯЕМ createKeywordsMenu - больше не нужен

    @Override
    public SendMessage createInfoMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📋 *Информация*\n\n" + TextConstants.INFO_TEXT.getText());
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Override
    public SendMessage createContactsMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📞 *Контакты*\n\n" + TextConstants.CONTACTS_TEXT.getText());
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    // ДОБАВЛЯЕМ НОВЫЙ МЕТОД ДЛЯ СТАТИСТИКИ
    public SendMessage createStatsMenu(Long chatId) {
        User user = userService.findByTelegramChatId(chatId);
        String stats = "📊 *Ваша статистика*\n\n";

        if (user != null) {
            stats += "👤 Логин: " + user.getUsername() + "\n";
            stats += getSubscriptionStatus(user.getUsername()) + "\n";
            stats += "*Генерации в этом месяце:*\n";
            stats += "🎨 Изображений: 0\n";
            stats += "🎥 Видео: 0\n";
        } else {
            stats += "❌ Данные не найдены";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(stats);
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private String getSubscriptionStatus(String username) {
        return subscriptionService.getSubscriptionStatus(username);
    }

}