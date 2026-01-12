package org.example.nanobananaprobot.bot.keyboards;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.constants.TextConstants;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
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

    private final GenerationBalanceService balanceService; /* ЗАМЕНЯЕМ*/

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

        String status = "";
        if (user != null) {
            int imageBalance = balanceService.getImageBalance(user.getId());
            int videoBalance = balanceService.getVideoBalance(user.getId());
            status = "🎨 Баланс изображений: " + imageBalance + "\n" +
                    "🎥 Баланс видео: " + videoBalance + "\n\n";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        if (!afterGeneration) {
            message.setText("🏠 *Главное меню*\n\n" + status + "Выберите действие:");
        } else {
            message.setText("✅ *Генерация завершена!*\n\n" + status + "Выберите следующее действие:");
        }
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        /* ПЕРВАЯ СТРОЧКА: Основная генерация */
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎨 Сгенерировать изображение"));
        row1.add(new KeyboardButton("✏️ Редактировать фото"));  // НОВАЯ КНОПКА

        /* ВТОРАЯ СТРОЧКА: Дополнительные функции */
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⚙️ Настройки"));           // НОВАЯ КНОПКА
        row2.add(new KeyboardButton("🎥 Сгенерировать видео"));

        /* ТРЕТЬЯ СТРОЧКА: Покупки и баланс */
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🛒 Купить генерации"));
        row3.add(new KeyboardButton("📊 Мой баланс"));

        /* ЧЕТВЕРТАЯ СТРОЧКА: Информация */
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("📋 Информация"));
        row4.add(new KeyboardButton("📞 Контакты"));

        /* ПЯТАЯ СТРОЧКА: Выход */
        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("❌ Выйти"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);

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

    /* ДОБАВЛЯЕМ НОВЫЙ МЕТОД ДЛЯ СТАТИСТИКИ*/
    @Override
    public SendMessage createStatsMenu(Long chatId) {
        User user = userService.findByTelegramChatId(chatId);
        String stats = "📊 *Ваша статистика*\n\n";

        if (user != null) {
            stats += "👤 Логин: " + user.getUsername() + "\n";

            /* Получаем баланс из нового сервиса*/
            int imageBalance = balanceService.getImageBalance(user.getId());
            int videoBalance = balanceService.getVideoBalance(user.getId());

            stats += "🎨 Баланс изображений: " + imageBalance + "\n";
            stats += "🎥 Баланс видео: " + videoBalance + "\n\n";

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
        /* Заменяем на получение баланса*/
        User user = userService.findUserByUsername(username);
        if (user == null) return "❌ Пользователь не найден";

        int imageBalance = balanceService.getImageBalance(user.getId());
        int videoBalance = balanceService.getVideoBalance(user.getId());

        return "🎨 Изображений: " + imageBalance + "\n" +
                "🎥 Видео: " + videoBalance;
    }

    @Override
    public SendMessage createImagePackagesMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        String text = "🎨 *Пакеты генерации изображений*\n\n";
        text += "💥 Чем больше генераций вы покупаете, тем выше скидка.\n\n";
        text += "Выберите желаемый тариф 👇\n\n";
        text += "3 генерации - 39₽ | 13₽ за генерацию\n";
        text += "10 генераций - 99₽ | 10₽ за генерацию\n";
        text += "50 генераций - 449₽ | 9₽ за генерацию\n";
        text += "100 генераций - 799₽ | 8₽ за генерацию\n";
        text += "300 генераций - 2099₽ | 7₽ за генерацию\n\n";
        text += "*Выберите количество:*";

        message.setText(text);
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        /* Каждый пакет в отдельной строке*/
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("3 генерации - 39₽"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("10 генераций - 99₽"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("50 генераций - 449₽"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("100 генераций - 799₽"));

        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("300 генераций - 2099₽"));

        KeyboardRow row6 = new KeyboardRow();
        row6.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);
        rows.add(row6);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Override
    public SendMessage createVideoPackagesMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        String text = "🎥 *Пакеты генерации видео*\n\n";
        text += "Выберите желаемый тариф 👇\n\n";
        text += "1 видео - 50₽\n";
        text += "5 видео - 225₽ (45₽/видео)\n";
        text += "10 видео - 399₽ (40₽/видео)\n\n";
        text += "*Выберите количество:*";

        message.setText(text);
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("1 видео - 50₽"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("5 видео - 225₽"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("10 видео - 399₽"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

}
