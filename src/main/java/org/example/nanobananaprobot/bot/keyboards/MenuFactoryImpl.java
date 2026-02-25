package org.example.nanobananaprobot.bot.keyboards;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.constants.TextConstants;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.InputStream;
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

    private final TelegramService telegramService;

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
        row1.add(new KeyboardButton("🏠 Старт"));

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
            int tokensBalance = balanceService.getTokensBalance(user.getId());
            status = "🎨 Баланс токенов: " + tokensBalance + " (" + (tokensBalance * 5) + " ₽)\n\n";
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
        row1.add(new KeyboardButton("🖼️ Объединить изображения"));  /* НОВАЯ КНОПКА*/

        /* ВТОРАЯ СТРОЧКА: Дополнительные функции */

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("✏️ Редактировать изображение"));  /* НОВАЯ КНОПКА*/
        /*row2.add(new KeyboardButton("🎥 Сгенерировать видео"));*/  /*пока убираем видео*/
        row2.add(new KeyboardButton("📋 Примеры промптов"));

        /* ТРЕТЬЯ СТРОЧКА: Покупки и баланс */

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("⚙️ Настройки"));           /* НОВАЯ КНОПКА*/
        row3.add(new KeyboardButton("🛒 Купить генерации"));

        /* ЧЕТВЕРТАЯ СТРОЧКА: Информация */

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("📊 Мой баланс"));
        row4.add(new KeyboardButton("📋 Информация"));

        /* ПЯТАЯ СТРОЧКА: Выход */

        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("📞 Контакты"));
        row5.add(new KeyboardButton("🏠 Главное меню"));

        /* ШЕСТАЯ СТРОЧКА: Выход */

        KeyboardRow row6 = new KeyboardRow();
        row6.add(new KeyboardButton("❌ Выйти"));

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

            int tokensBalance = balanceService.getTokensBalance(user.getId());

            stats += "💰 Баланс токенов: " + tokensBalance + "\n";
            stats += "💵 Стоимость: " + (tokensBalance * 5) + " ₽\n\n";

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

        int tokensBalance = balanceService.getTokensBalance(user.getId());
        return "💰 Токенов: " + tokensBalance + " (" + (tokensBalance * 5) + " ₽)";
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

    @Override
    public SendMessage createTokenPackagesMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        String text = "💰 *Пакеты токенов*\n\n";
        text += "1 токен = 5 ₽\n\n";
        text += "Пакеты по расчётам :\n";
        text += "• 5 токенов - 25₽\n";
        text += "• 10 токенов - 50₽\n";
        text += "• 30 токенов - 150₽\n";
        text += "• 50 токенов - 250₽\n";
        text += "• 100 токенов - 500₽\n\n";
        text += "*Стоимость генераций:*\n";
        text += "• 1K: 3 токена (15₽)\n";
        text += "• 2K: 4 токена (20₽)\n";
        text += "• 4K: 5 токенов (25₽)\n";
        text += "• Редактирование: +1 токен\n";
        text += "• Слияние: база +1 токен за фото\n\n";
        text += "Выберите пакет:";

        message.setText(text);
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        /* Пакеты токенов*/

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("5 токенов - 25₽"));
        row1.add(new KeyboardButton("10 токенов - 50₽"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("30 токенов - 150₽"));
        row2.add(new KeyboardButton("50 токенов - 250₽"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("100 токенов - 500₽"));

        KeyboardRow rowBack = new KeyboardRow();
        rowBack.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(rowBack);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Override
    public SendMessage createPromptsExamplesMenu(Long chatId) {
        String[][] examples = {
                {
                        "helicopter.jpg",
                        "🚁 *Пример: Фото с вертолётом*\n\n```\nСТРОГО сохранить внешность 1:1. Не изменять черты лица, возраст, пропорции.\nЛокация/фон: аэродром с вертолётом на закате;\nосвещение: тёплый боковой свет слева, мягкие тени;\nракурс/кадр: средний план тела, лёгкий нижний ракурс;\nпоза: человек в чёрном спортивном костюме сидит на пороге открытой дверцы вертолета;\nстиль: фотореализм, кино; глубина резкости: средняя;\nфокусное расстояние: 50 мм; атмосферная спокойная энергия кадра.\n```"
                },
                {
                        "lake.jpg",
                        "🏞️ *Пример: Горное озеро*\n\n```\nЛокация: горное озеро на рассвете, отражение гор в воде;\nосвещение: мягкий рассеянный свет, туман над водой;\nстиль: фотореализм, национальный парк;\nэлементы: сосны по берегам, скалы, кристально чистая вода;\nнастроение: спокойствие, уединение, природная гармония.\n```"
                },
                {
                        "cyberpunk.jpg",
                        "🎭 *Пример: Киберпанк портрет*\n\n```\nСТРОГО сохранить лицо 1:1;\nстиль: киберпанк, неон-нуар;\nэлементы: голографические элементы на лице, неоновая подсветка;\nфон: дождливый мегаполис ночью, отражения в лужах;\nосвещение: контровой неоновый свет, цветные тени;\nнастроение: загадочность, технологичность.\n```"
                },
                {
                        "pizza.jpg",
                        "🍕 *Пример: Предметная съёмка*\n\n```\nОбъект: пицца с пепперони на деревянном столе;\nосвещение: тёплый боковой свет, аппетитные блики;\nракурс: вид сверху, немного под углом;\nэлементы: тянущийся сыр, базилик, пармезан, соус в пиале рядом;\nстиль: фуд-фотография, сочные цвета, глубина резкости малая;\nнастроение: аппетитно, уютно.\n```"
                },
                {
                        "fantazyAnimal.jpg",
                        "🐱 *Животные в стиле фэнтези*\n\n```\nЖивотное: пушистый рыжий кот;\nлокация: волшебный лес с грибами и светлячками;\nэлементы: у кота маленькие феи-крылышки, на голове цветочная корона;\nосвещение: магическое свечение от грибов, мягкий рассеянный свет;\nстиль: сказочная иллюстрация, детализированно, акварельные тона;\nнастроение: волшебство, доброта.\n```"
                },
                {
                        "modern.jpg",
                        "🏛️ *Архитектура в стиле модерн*\n\n```\nздание: небоскрёб с зелёным фасадом (вертикальное озеленение);\nлокация: деловой центр города, утро;\nракурс: снизу вверх, слегка искажённая перспектива;\nосвещение: солнечные лучи пробиваются сквозь листву на фасаде;\nэлементы: отражение облаков в стекле, люди внизу, облака;\nстиль: архитектурная фотография, урбанистика, эко-технологии.\n```"
                },
                {
                        "fonAbstraction.jpg",
                        "🎨 *Абстракция для фона*\n\n```\nСтиль: абстрактный экспрессионизм;\nцвета: синий, золотой, изумрудный, переливы;\nтекстура: мазки кистью, брызги, мраморные разводы;\nэлементы: геометрические формы, плавные линии, градиенты;\nнастроение: динамика, роскошь, глубина;\nиспользование: для фона презентации или обоев.\n```"
                },
                {
                        "advice.jpg",
                        "💡 *Советы*\n\n• Указывайте ЧТО сохранить (лицо, поза, одежда)\n• Описывайте ЛОКАЦИЮ и ОСВЕЩЕНИЕ\n• Добавляйте ДЕТАЛИ (одежда, предметы, настроение)\n• Указывайте СТИЛЬ (фотореализм, арт, кино и т.д.)\n\n*Просто скопируйте промпт и используйте в боте!* 🚀"
                }
        };

        for (String[] example : examples) {
            telegramService.sendPromptExample(chatId, example[0], example[1]);
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📋 *Все примеры отправлены!*\n\nВыберите действие:");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow rowBack = new KeyboardRow();
        rowBack.add(new KeyboardButton("🔙 Назад"));

        rows.add(rowBack);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

}
