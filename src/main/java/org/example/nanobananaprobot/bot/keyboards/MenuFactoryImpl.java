package org.example.nanobananaprobot.bot.keyboards;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.constants.TextConstants;
import org.example.nanobananaprobot.bot.service.TelegramService;
import org.example.nanobananaprobot.domain.dto.TokenConfig;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final TokenConfig tokenConfig;

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
            /*status = "🎨 *Баланс:* " + tokensBalance + " токенов (" + (tokensBalance * 5) + " ₽)\n\n";*/
            status = "🎨 *Баланс:* " + tokensBalance + " токенов (" + (tokensBalance * tokenConfig.getPriceRub()) + " ₽)\n\n";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(status + "👇 *Выбери действие:*");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        /* Первая строка — самое главное*/
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎨 Создать изображение"));

        /* Вторая строка — вспомогательное*/
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💰 Пополнить баланс"));
        row2.add(new KeyboardButton("📋 Примеры промптов"));

        /* Третья строка — настройки и инфо*/
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("⚙️ Настройки"));
        row3.add(new KeyboardButton("❓ Помощь"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }
    /*СТАРЫЙ МЕТОД*/

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
            /*stats += "💵 Стоимость: " + (tokensBalance * 5) + " ₽\n\n";*/
            stats += "💵 Стоимость: " + (tokensBalance * tokenConfig.getPriceRub()) + " ₽\n\n";

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
        /*return "💰 Токенов: " + tokensBalance + " (" + (tokensBalance * 5) + " ₽)";*/
        return "💰 Токенов: " + tokensBalance + " (" + (tokensBalance * tokenConfig.getPriceRub()) + " ₽)";
    }

    /*СТАРЫЙ МЕТОД*/

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

    /*СТАРЫЙ МЕТОД*/

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

        int pricePerToken = tokenConfig.getPriceRub();

        String text = "💰 *Пакеты токенов*\n\n";
        text += "1 токен = " + pricePerToken + " ₽\n\n";
        text += "Пакеты по расчётам:\n";
        text += "• 5 токенов - " + (5 * pricePerToken) + "₽\n";
        text += "• 10 токенов - " + (10 * pricePerToken) + "₽\n";
        text += "• 30 токенов - " + (30 * pricePerToken) + "₽\n";
        text += "• 50 токенов - " + (50 * pricePerToken) + "₽\n";
        text += "• 100 токенов - " + (100 * pricePerToken) + "₽\n\n";
        text += "*Стоимость генераций:*\n\n";
        text += "*По текстовому описанию:*\n";
        text += "• 1K: 3 токена (" + (3 * pricePerToken) + "₽)\n";
        text += "• 2K: 4 токена (" + (4 * pricePerToken) + "₽)\n";
        text += "• 4K: 5 токенов (" + (5 * pricePerToken) + "₽)\n\n";
        text += "*По текстовому описанию и фото:*\n";
        text += "• 1K: 4 токена (" + (4 * pricePerToken) + "₽)\n";
        text += "• 2K: 5 токенов (" + (5 * pricePerToken) + "₽)\n";
        text += "• 4K: 6 токенов (" + (6 * pricePerToken) + "₽)\n\n";
        text += "*Слияние двух фото и текстовое описание:*\n";
        text += "• 1K: 5 токенов (" + (5 * pricePerToken) + "₽)\n";
        text += "• 2K: 6 токенов (" + (6 * pricePerToken) + "₽)\n";
        text += "• 4K: 7 токенов (" + (7 * pricePerToken) + "₽)\n\n";
        text += "*Слияние более двух фото и текстовое описание:*\n";
        text += "• + 1 токен за каждое дополнительное фото\n\n";
        text += "Выберите пакет:";

        message.setText(text);
        message.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        /* Пакеты по 2 в строке*/

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        addButton(row1, "5 токенов - " + (5 * pricePerToken) + "₽", "token_5");
        addButton(row1, "10 токенов - " + (10 * pricePerToken) + "₽", "token_10");

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        addButton(row2, "30 токенов - " + (30 * pricePerToken) + "₽", "token_30");
        addButton(row2, "50 токенов - " + (50 * pricePerToken) + "₽", "token_50");

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        addButton(row3, "100 токенов - " + (100 * pricePerToken) + "₽", "token_100");

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        addButton(row4, "🔙 Назад", "back_to_menu");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private void addButton(List<InlineKeyboardButton> row, String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        row.add(button);
    }

    @Override
    public SendMessage createPromptsExamplesMenu(Long chatId) {
        String[][] examples = {
                {
                        "1_1.jpg",
                        "1_2.jpg",
                        "*Пример: Фото на поле с маками*\n\n```\nСохранить внешность на 100%. Не изменять черты лица, пропорции. Фото сделано на одноразовую камеру со вспышкой. Девушка на фоне бескрайнего поля с маками. У нее волнистые волосы, развивающиеся от ветра, падают на лицо (motion blur волос). Кожа с мягким сиянием хайлайтера, глянцевые губы, красные ногти. Она идет по полю, держась за руку с другим человеком, улыбается; на переднем плане видна только рука другого человека, на которой золотые часы и браслет, создавая перспективу «следуй за мной». Женщина одета в яркое, красное платье с открытой спиной, завязанное на шее лентами, струящаяся и романтичная. Композиция подчеркивает движение, связь, соединенные руки должны вести зрителя в кадр. Чистые белые облака на фоне голубого неба и будто только надвигающиеся тучи. Фото с яркой вспышкой.\n\n```"
                },
                {
                        "2_1.jpg",
                        "2_2.jpg",
                        "*Пример: Стильное фото в кафе*\n\n```\nСохранить внешность 1:1. Не изменять черты лица, пропорции. Ультрареалистичное фото на iPhone 15 Pro в эстетике роскошного кафе, естественная обработка смартфона, легкая небрежность кадра. Стильная женщина сидит за белым мраморным столиком на черном кожаном диване террасы дорогого ресторана. Расслабленная элегантная поза: одна рука касается затылка, другая держит стакан апельсинового сока со стеклянной трубочкой, взгляд слегка в сторону камеры. На ней облегающий белый топ в рубец с высоким вырезом и открытыми плечами, без рукавов, элегантные белые брюки с высокой талией. На ней винтажные овальные очки, кольца, небольшие серьги-кольца, на одной руке часы, на другой браслеты из серебра и золота, браслет из бамбука. Рядом дизайнерская сумка-тоут. Светлая терраса ресторана, зеленая изгородь на фоне, теплый дневной свет. Высокий хвост и мягкие пряди у лица. Естественные тени, легкая зернистость, эстетика Pinterest, реалистичная атмосфера кафе.\n\n```"
                },
                {
                        "3_1.jpg",
                        "3_2.jpg",
                        "*Пример: Трендовое фото около магазина с кумирами *\n\n```\nФото 1: Сохранить внешность на 100%. Не изменять черты лица, пропорции. Реалистичное фото на iPhone, без HDR и фильтров, эффект необработанного кадра с легкой зернистостью и естественными несовершенствами. Ночь, зона отдыха возле местной «Пятерочки» рядом со стеклянной стеной. Яркие флуоресцентные лампы навеса создают резкие тени. На фоне большие окна магазина с логотипом, полками с напитками и закусками, слева темная улица с мотоциклами и тусклыми фонарями. На небольшом металлическом столе несколько закусок, русские газировки, lays, шоколадный и вишневый тортик, вокруг простые металлические стулья. В кадре человек с фото сидит слева, справа полукругом сидят Алла Пугачёва, Филипп Киркоров, Сергей Лазарев и Лолита Милявская сидят рядом за столом и непринужденно разговаривают. Все выглядят естественно, вовлечены в беседу, не смотрят в камеру. Атмосфера обычной вечерней встречи друзей, современный casual стиль одежды.\n\n Фото 2: Сохранить внешность на 100%. Не изменять черты лица, пропорции. Реалистичное фото на iPhone, без HDR и фильтров, эффект необработанного кадра с легкой зернистостью и естественными несовершенствами. Ночь, зона отдыха возле местной «Пятерочки» рядом со стеклянной стеной. Яркие флуоресцентные лампы навеса создают резкие тени. На фоне большие окна магазина с логотипом, полками с напитками и закусками, слева темная улица с мотоциклами и тусклыми фонарями. На небольшом металлическом столе несколько закусок, русские газировки, lays, шоколадный и вишневый тортик, вокруг простые металлические стулья. В кадре человек с фото сидит слева в casual одежде, справа полукругом сидят реперы 50 cent, Drake, Kanye West, Cardi B рядом за столом и непринужденно разговаривают. Все выглядят естественно, вовлечены в беседу, не смотрят в камеру. Атмосфера обычной вечерней встречи друзей, современный casual стиль одежды.\n\n ```"
                },
                {
                        "4_1.jpg",
                        "4_2.jpg",
                        "*Пример: Фото с нижнего ракурса*\n\n```\nСохранить внешность на 100%. Не изменять черты лица, пропорции. Крупное селфи от первого лица, ракурс снизу вверх. Телефон в кадре не видно. Девушка смотрит в камеру с уверенным, слегка томным взглядом. Рука подносит аппликатор блеска к нижней губе, видны длинные молочные ногти. Глянцевые розово-нюдовые губы слегка приоткрыты. Выразительные глаза, длинные ресницы, четкие стрелки, нюдовые тени с легким шиммером, гладкая кожа. Волосы объемные, слегка растрепанные, частично закрывают лицо. Черный корсетный топ без бретелей с полупрозрачными рукавами. Золотая цепочка на шее, минималистичные висячие серьги. Темный размытый фон и потолок. Основное освещение тусклый свет экрана телефона, глубокие тени. Фотореализм, iPhone 16, легкий цифровой шум, мягкая резкость, приглушенные цвета, легкая зернистость.\n\n```"
                },
                {
                        "5_1.jpg",
                        "5_2.jpg",
                        "*Фото крупного плана*\n\n```\nСохранить внешность на 100%. Не изменять черты лица, пропорции. Кинематографический сверхдетализированный цветной портрет 8K, студия, минимализм, крупный план. Формат 3:4. Девушка слегка подается вперёд, подбородок мягко опирается на руку; задумчивое, спокойное выражение, прямой глубокий взгляд в камеру. Волосы частично закрывают лицо, пряди обрамляют глаза и губы. Топ на тонких бретелях. Мягкий направленный студийный свет, высокий контраст, акцент на текстуре кожи и игре света/тени. Съёмка: Canon EOS R5, 85mm, f/1.8.\n\n```"
                },
                {
                        "6_1.jpg",
                        "6_2.jpg",
                        "Пример: *Фото с букетом в квартире*\n\n```\nСохранить внешность на 100%. Не изменять черты лица, пропорции.  Фотореалистичный крупный снимок женщины по пояс, стоящей в помещении с минималистичным интерьером. Она держит в руках гигантский, невероятно пышный букет из сотен розовых и белых пионовидных роз. Букет настолько большой, что почти закрывает ее верхнюю часть тела. Женщина стоит вполоборота, прижимая букет к себе, легкая улыбка. У нее волнистые волосы. Она одета в белый облегающий лонгслив с рукавами, и светло-голубые джинсы с высокой посадкой. Задний план — это светлые стены квартиры и темная дверь, что создает контраст и фокусирует внимание на модели и цветах. Освещение мягкое и равномерное, подчеркивающее нежность цветов и создающее атмосферу радости, роскоши и праздника. Фото в золотой час и кинематографический эффект с добавлением зерна. Стиль RAW.\n\n```"
                },
                {
                        "7_1.jpg",
                        "7_2.jpg",
                        "Пример: Мужское фото с зажигалками\n\n```\nСохранить внешность 1:1. Фото — единственный эталон. Ультрареалистичный кинематографичный ночной портрет мужчины с сигаретой. Мужчина по центру кадра, слегка опустив взгляд, подносит сигарету к губам, спокойное выражение. На нем черная майка без рукавов, тонкая цепочка или кольцо. В кадр с нижних краёв входят 4–6 разных рук с зажжёнными зажигалками, пламя образует вокруг лица хаотичный полукруг. Тёмная ночная сцена, единственный источник света огонь зажигалок. Волосы слегка растрепаны. Тёплый оранжевый свет неравномерно освещает лицо и пальцы, глубокие тени, высокий контраст. Ручная съёмка крупным планом, малая глубина резкости, фокус на лице, руки и фон слегка размыты. Кинематографичное зерно, low-light шум, лёгкий motion blur на руках. Естественная текстура кожи, единый grain/noise по всему кадру. Фон почти чёрный или тёмно-синий, возможны мягкие боке. Атмосфера интимная, сырая, gritty.\n```"
                },
               /* {
                        "cyberpunk.jpg",
                        "cyberpunk2.jpg",
                        "🎭 *Пример: Киберпанк портрет*\n\n```\nСТРОГО сохранить лицо 1:1;\nстиль: киберпанк, неон-нуар;\nэлементы: голографические элементы на лице, неоновая подсветка;\nфон: дождливый мегаполис ночью, отражения в лужах;\nосвещение: контровой неоновый свет, цветные тени;\nнастроение: загадочность, технологичность.\n```"
                },*/
                {
                        "advice.jpg",
                        "advice2.jpg",
                        "💡 *Советы*\n\n• Указывайте ЧТО сохранить (лицо, поза, одежда)\n• Описывайте ЛОКАЦИЮ и ОСВЕЩЕНИЕ\n• Добавляйте ДЕТАЛИ (одежда, предметы, настроение)\n• Указывайте СТИЛЬ (фотореализм, арт, кино и т.д.)\n\n*Просто скопируйте промпт и используйте в боте!* 🚀"
                }
        };

        for (String[] example : examples) {
            try {
                InputStream is1 = getClass().getClassLoader().getResourceAsStream("examples/" + example[0]);
                InputStream is2 = getClass().getClassLoader().getResourceAsStream("examples/" + example[1]);

                if (is1 != null && is2 != null) {
                    byte[] img1 = is1.readAllBytes();
                    byte[] img2 = is2.readAllBytes();

                    List<InputMedia> mediaList = new ArrayList<>();

                    InputMediaPhoto media1 = new InputMediaPhoto();
                    media1.setMedia(new ByteArrayInputStream(img1), example[0]);
                    media1.setCaption("");  /* подпись пустая, чтобы не было ошибок длины и markdown */
                    mediaList.add(media1);

                    InputMediaPhoto media2 = new InputMediaPhoto();
                    media2.setMedia(new ByteArrayInputStream(img2), example[1]);
                    mediaList.add(media2);

                    SendMediaGroup sendMediaGroup = new SendMediaGroup();
                    sendMediaGroup.setChatId(chatId.toString());
                    sendMediaGroup.setMedias(mediaList);

                    telegramService.sendMediaGroup(sendMediaGroup);

                    /* Отправляем текст примера отдельным сообщением — без Markdown, чтобы избежать ошибок разметки */
                    telegramService.sendMessage(chatId, example[2], "Markdown");

                    Thread.sleep(500);
                }
            } catch (Exception e) {
                log.error("Error sending example: {}", example[0], e);
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📋 *Все примеры отправлены!*\n\nВыберите действие:");
        message.setParseMode("Markdown");

        return message;
    }


}
