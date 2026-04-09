package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.keyboards.MenuFactory;
import org.example.nanobananaprobot.bot.service.*;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.CometApiService;
import org.example.nanobananaprobot.service.CostCalculatorService;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import org.example.nanobananaprobot.domain.dto.ImageConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHandlerImpl implements MessageHandler {

    private final AuthService authService;
    private final PaymentHandler paymentHandler;
    private final GenerationService generationService;
    private final UserStateManager stateManager;
    private final UserServiceData userService;
    private final GenerationBalanceService balanceService;
    private final TelegramService telegramService;
    private final MenuFactory menuFactory;

    private final CometApiService cometApiService;

    private final CostCalculatorService costCalculatorService; /* Добавляем*/

    @Override
    public void handleTextMessage(Message message) {
        if (message == null || message.getText() == null) {
            log.debug("Ignoring non-text message from chatId: {}",
                    message != null ? message.getChatId() : "N/A");
            return;
        }

        Long chatId = message.getChatId();
        String text = message.getText();

        try {
            String userState = stateManager.getUserState(chatId);
            log.debug("Handling message - ChatId: {}, Text: {}, State: {}", chatId, text, userState);

            /* 1. Обрабатываем системные команды*/
            switch (text) {
                case "/start", "🏠 Старт", "Начать → /start" -> {
                    handleStartCommand(chatId, message.getFrom());
                    return;
                }

                case "Главное меню → /menu" -> {
                    showUserInfo(chatId);
                    /*telegramService.sendMessage(menuFactory.showMainMenuCompact(chatId));*/
                    return;
                }

                case "Купить генерации → /buy" -> {
                    telegramService.sendMessage(menuFactory.createTokenPackagesMenu(chatId));
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_TOKEN_PACKAGE);
                    return;
                }

                case "Пригласить друзей → /invite" -> {

                    /* TODO: реализовать приглашение друзей*/

                    telegramService.sendMessage(chatId, "🔗 Ваша реферальная ссылка:\nhttps://t.me/ваш_бот?start=ref_" + chatId);
                    return;
                }

                case "📋 Меню" -> {
                    showMainMenuCompact(chatId);
                    return;
                }

               /* case "🔙 Назад" -> {
                    showMainMenuCompact(chatId);
                    stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                    return;
                }*/

                case "🔙 Назад" -> {
                    String currentState = stateManager.getUserState(chatId);

                    /* Если мы в меню выбора формата или качества — возвращаемся в настройки*/
                    if (UserStateManager.STATE_WAITING_ASPECT_RATIO.equals(currentState) ||
                            UserStateManager.STATE_WAITING_RESOLUTION.equals(currentState)) {
                        showSettingsMenu(chatId);
                    } else {
                        /* В остальных случаях — в главное меню*/
                        showMainMenuCompact(chatId);
                        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                    }
                    return;
                }

            }

            /* 2. Обработка состояний ввода*/
            if (handleInputStates(chatId, text, userState)) {
                return;
            }

            /* 3. Если не системная команда и не состояние — просто показываем меню*/
            showMainMenuCompact(chatId);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
            telegramService.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте еще раз.");
        }
    }

    private void showUserInfo(Long chatId) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        int tokensBalance = balanceService.getTokensBalance(user.getId());
        int totalGenerations = balanceService.getTotalGenerations(user.getId()); /* нужно добавить*/

        String info = "📊 *Ваша статистика*\n\n" +
                "👤 ID: `" + chatId + "`\n" +
                "💰 Токенов: " + tokensBalance + "\n" +
                "🎨 Генераций: " + totalGenerations + "\n\n" +
                "👇 *Выберите действие:*";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(info);
        message.setParseMode("Markdown");

        /* Показываем меню после информации*/
        telegramService.sendMessage(message);
        telegramService.sendMessage(menuFactory.showMainMenuCompact(chatId));
    }

    private boolean handleInputStates(Long chatId, String text, String userState) {

        /* ДОБАВЬТЕ ЭТУ ПРОВЕРКУ*/
        if (text == null) {
            log.error("handleInputStates received NULL text! ChatId: {}, State: {}", chatId, userState);
            return true;
        }

        /* Если текст — это команда меню — не обрабатываем как промпт*/
        if (text.startsWith("/") || text.equals("Купить генерации → /buy") ||
                text.equals("Главное меню → /menu") || text.equals("Пригласить друзей → /invite") ||
                text.equals("Начать → /start")) {
            return false; /* не блокируем, пусть обрабатывается как команда*/
        }

        /* БЛОКИРОВКА КНОПОК ВО ВРЕМЯ ВВОДА*/
        if (userState.equals(UserStateManager.STATE_WAITING_IMAGE_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_VIDEO_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_USERNAME) ||
                userState.equals(UserStateManager.STATE_WAITING_PASSWORD) ||
                userState.equals(UserStateManager.STATE_REGISTER_USERNAME) ||
                userState.equals(UserStateManager.STATE_REGISTER_PASSWORD) ||
                userState.equals(UserStateManager.STATE_REGISTER_EMAIL) ||
                userState.equals(UserStateManager.STATE_WAITING_PAYMENT_ID) ||
                userState.equals(UserStateManager.STATE_WAITING_EDIT_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_QUALITY_SETTINGS) ||
                userState.equals(UserStateManager.STATE_WAITING_MERGE_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD) ||
                userState.equals(UserStateManager.STATE_WAITING_TOKEN_PACKAGE)  /* ← ДОБАВИТЬ если нужно*/
        ) {
            if (isMenuCommand(text)) {
                telegramService.sendMessage(chatId, "❌ Завершите текущий процесс ввода");
                return true;
            }
        }

        if ("❌ Выйти".equals(text)) {
            authService.handleLogout(chatId);
            return true;
        }

        /* ОСТАЛЬНЫЕ СОСТОЯНИЯ ВВОДА*/
        switch (userState) {

            case UserStateManager.STATE_REGISTER_EMAIL:
                authService.handleEmailInput(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_PAYMENT_ID:
                paymentHandler.handlePaymentCheck(chatId, text);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                return true;

            case UserStateManager.STATE_WAITING_USERNAME:
                authService.handleUsernameInput(chatId, text, false);
                return true;

            case UserStateManager.STATE_WAITING_PASSWORD:
                authService.handlePasswordInput(chatId, text, false);
                return true;

            case UserStateManager.STATE_REGISTER_USERNAME:
                authService.handleUsernameInput(chatId, text, true);
                return true;

            case UserStateManager.STATE_REGISTER_PASSWORD:
                authService.handlePasswordInput(chatId, text, true);
                return true;

            case UserStateManager.STATE_WAITING_IMAGE_PROMPT:
                generationService.handleImageGeneration(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_VIDEO_PROMPT:
                generationService.handleVideoGeneration(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_USER_INPUT : {
                handleUserInput(chatId, text, null);                      /* пока без фото, потом добавим*/
                return true;
            }

            case UserStateManager.STATE_WAITING_SETTINGS : {
                handleSettingsSelection(chatId, text);
                return true;
            }

            case UserStateManager.STATE_WAITING_ASPECT_RATIO : {
                handleAspectRatioSelection(chatId, text);
                return true;
            }

            case UserStateManager.STATE_WAITING_RESOLUTION : {
                handleResolutionSelection(chatId, text);
                return true;
            }

            /* ОБРАБОТКА ВЫБОРА ПАКЕТОВ*/
            case UserStateManager.STATE_WAITING_PACKAGE_TYPE:
                if ("🎨 Изображения".equals(text)) {
                    telegramService.sendMessage(menuFactory.createImagePackagesMenu(chatId));
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PACKAGE);
                } else if ("🎥 Видео".equals(text)) {
                    telegramService.sendMessage(menuFactory.createVideoPackagesMenu(chatId));
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_VIDEO_PACKAGE);
                } else {
                    showMainMenuCompact(chatId);
                }
                return true;

            case UserStateManager.STATE_WAITING_IMAGE_PACKAGE:
                handleImagePackageSelection(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_VIDEO_PACKAGE:
                handleVideoPackageSelection(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_TEST_PROMPT:
                generationService.testHiggsfieldGeneration(chatId, text);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                return true;

           /* case UserStateManager.STATE_WAITING_EDIT_PROMPT:
                handleEditPromptInput(chatId, text);
                return true;*/

            case UserStateManager.STATE_WAITING_QUALITY_SETTINGS:
                handleQualitySettingsInput(chatId, text);
                return true;

            /*case UserStateManager.STATE_WAITING_MERGE_PROMPT:

                *//* Добавить проверку на системные кнопки*//*

                if ("❌ Отмена слияния".equals(text) || "🏠 Главное меню".equals(text)) {
                    stateManager.clearMultipleImages(chatId);
                    stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                    if ("🏠 Главное меню".equals(text)) {
                        showMainMenuCompact(chatId);
                    } else {
                        telegramService.sendMessage(chatId, "❌ Слияние отменено.");
                        showMainMenuCompact(chatId);
                    }
                    return true;
                }
                *//* Если не системная кнопка - обрабатываем как промпт*//*

                handleMergePromptInput(chatId, text);
                return true;*/

            /* ВАЖНО: Добавляем обработку состояния ожидания загрузки нескольких фото*/

            case UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD:
                return handleMultipleImagesUploadState(chatId, text);

            case UserStateManager.STATE_WAITING_TOKEN_PACKAGE:
                handleTokenPackageSelection(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_EXTRA_PHOTO:
                /* Добавляем фото в коллекцию*/
                List<byte[]> images = stateManager.getMultipleImages(chatId);
                byte[] newPhoto = stateManager.getUploadedImage(chatId);
                if (newPhoto != null) {
                    images.add(newPhoto);
                    stateManager.clearUploadedImage(chatId);
                }

                if (images.size() >= 2) {
                    sendMergeContinueButton(chatId, images.size());
                } else {
                    telegramService.sendMessage(chatId, "📸 Отправьте ещё одно фото (нужно минимум 2)");
                }
                return true;

            case UserStateManager.STATE_WAITING_EDIT_PROMPT:
                handleEditPromptInput(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_MERGE_PROMPT:
                handleMergePromptInput(chatId, text);
                return true;

            default:
                return false;
        }
    }

    private void sendMergeContinueButton(Long chatId, int count) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton continueBtn = new InlineKeyboardButton();
        continueBtn.setText("✅ Продолжить (" + count + " фото)");
        continueBtn.setCallbackData("merge_continue");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("cancel_photo");

        row.add(continueBtn);
        row.add(cancelBtn);
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📸 Загружено " + count + " фото. Нажмите 'Продолжить', чтобы ввести описание слияния");
        message.setReplyMarkup(inlineKeyboard);

        telegramService.sendMessage(message);
    }

    private void handleAspectRatioSelection(Long chatId, String text) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        switch (text) {
            case "📐 1:1 (Квадрат)" -> config.setAspectRatio("1:1");
            case "📐 16:9 (Широкий)" -> config.setAspectRatio("16:9");
            case "🎬 21:9 (Кино)" -> config.setAspectRatio("21:9");
            case "🖥️ 4:3 (Классический)" -> config.setAspectRatio("4:3");
            case "📱 9:16 (Сторис)" -> config.setAspectRatio("9:16");
            case "📄 2:3 (Портрет)" -> config.setAspectRatio("2:3");
            case "📷 3:2 (Фото)" -> config.setAspectRatio("3:2");
            case "📱 3:4 (Смартфон)" -> config.setAspectRatio("3:4");
            case "📄 4:5 (Вертикальный)" -> config.setAspectRatio("4:5");
            case "📊 5:4 (Соотношение 5:4)" -> config.setAspectRatio("5:4");
            case "🔙 Назад" -> {
                showSettingsMenu(chatId);
                return;
            }
            default -> {
                telegramService.sendMessage(chatId, "❌ Неизвестный формат");
                return;
            }
        }

        stateManager.saveConfig(chatId, config);
        showSettingsMenu(chatId);
    }

    private void handleResolutionSelection(Long chatId, String text) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        switch (text) {
            case "🖼️ 1K (Базовое)" -> config.setResolution("1K");
            case "🖼️ 2K (Качественное)" -> config.setResolution("2K");
            case "🖼️ 4K (Максимальное)" -> config.setResolution("4K");
            case "🔙 Назад" -> {
                showSettingsMenu(chatId);
                return;
            }
            default -> {
                telegramService.sendMessage(chatId, "❌ Неизвестное качество");
                return;
            }
        }

        stateManager.saveConfig(chatId, config);
        showSettingsMenu(chatId);
    }

    private void showAspectRatioSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📐 *Выберите соотношение сторон:*");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📐 1:1 (Квадрат)"));
        row1.add(new KeyboardButton("📐 16:9 (Широкий)"));
        row1.add(new KeyboardButton("🎬 21:9 (Кино)"));
        row1.add(new KeyboardButton("🖥️ 4:3 (Классический)"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📱 9:16 (Сторис)"));
        row2.add(new KeyboardButton("📄 2:3 (Портрет)"));
        row2.add(new KeyboardButton("📷 3:2 (Фото)"));
        row2.add(new KeyboardButton("📱 3:4 (Смартфон)"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📄 4:5 (Вертикальный)"));
        row3.add(new KeyboardButton("📊 5:4 (Соотношение 5:4)"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        telegramService.sendMessage(message);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_ASPECT_RATIO);
    }

    private void showResolutionSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🖼️ *Выберите качество изображения:*\n\n" +
                "• 1K — 3 токена (15 ₽)\n" +
                "• 2K — 4 токена (20 ₽)\n" +
                "• 4K — 5 токенов (25 ₽)");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🖼️ 1K (Базовое)"));
        row1.add(new KeyboardButton("🖼️ 2K (Качественное)"));
        row1.add(new KeyboardButton("🖼️ 4K (Максимальное)"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        telegramService.sendMessage(message);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_RESOLUTION);
    }

    private void startGenerationWithCurrentSettings(Long chatId) {
        /* Получаем пользователя из БД*/
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }
        Long userId = user.getId();

        /* Получаем сохранённые данные*/
        String prompt = stateManager.getTempPrompt(chatId);
        byte[] image = stateManager.getUploadedImage(chatId);
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        /* Устанавливаем режим перед использованием*/
        if (image != null) {
            config.setMode("edit");
        } else {
            config.setMode("generate");
        }

        if (prompt == null || prompt.isEmpty()) {
            telegramService.sendMessage(chatId, "❌ Промпт не найден. Начните заново.");
            showMainMenuCompact(chatId);
            return;
        }

        /* Проверяем баланс*/
        int requiredTokens = costCalculatorService.calculateTokens(config);
        if (!balanceService.hasEnoughTokens(userId, requiredTokens)) {
            telegramService.sendMessage(chatId, "❌ Недостаточно токенов!\n" +
                    "💰 Требуется: " + requiredTokens + " токенов (" + (requiredTokens * 5) + " ₽)\n" +
                    "🛒 Пополните баланс в магазине");
            return;
        }

        /* Определяем тип операции и запускаем*/
        if (image != null) {
            config.setMode("edit");  /* ← ДОБАВИТЬ*/
            /* Редактирование*/
            balanceService.useImageEdit(userId, config);
            startAsyncImageEdit(chatId, userId, image, prompt, config);
        } else {
            /* Генерация*/
            config.setMode("generate");  /* ← ДОБАВИТЬ*/
            balanceService.useImageGeneration(userId, config);
            generationService.startAsyncGeneration(chatId, userId, prompt);
        }

        /* Очищаем временные данные*/
        stateManager.clearTempData(chatId);
        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
    }

    private void handleUserInput(Long chatId, String text, List<byte[]> photos) {
        /* Сохраняем фото в stateManager*/
        if (photos != null && !photos.isEmpty()) {
            stateManager.saveUploadedImage(chatId, photos.get(0));
        }

        /* Сохраняем промпт*/
        if (text != null && !text.isEmpty()) {
            stateManager.setTempPrompt(chatId, text);
        }

        /* Переходим к выбору настроек*/
        showSettingsMenu(chatId);
    }

    private void handleSettingsSelection(Long chatId, String text) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        switch (text) {
            case "📐 Формат" -> showAspectRatioSelection(chatId);
            case "🖼️ Качество" -> showResolutionSelection(chatId);
            case "✅ Сгенерировать" -> startGenerationWithCurrentSettings(chatId);
            case "🔙 Отмена" -> {
                showMainMenuCompact(chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            }
            default -> telegramService.sendMessage(chatId, "❌ Неизвестная команда");
        }
    }

    @Override
    public void showSettingsMenu(Long chatId) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        /* Определяем режим по наличию фото*/
        byte[] image = stateManager.getUploadedImage(chatId);
        if (image != null) {
            config.setMode("edit");
        } else {
            config.setMode("generate");
        }

        String settingsText = "⚙️ *Настройки генерации*\n\n" +
                "Текущие параметры:\n" +
                "• Формат: " + config.getAspectRatio() + "\n" +
                "• Качество: " + config.getResolution() + "\n" +
                "• Стоимость: " + costCalculatorService.getDescription(config) + "\n\n" +
                "Выберите параметр для изменения:";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(settingsText);
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📐 Формат"));
        row1.add(new KeyboardButton("🖼️ Качество"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("✅ Сгенерировать"));
        row2.add(new KeyboardButton("🔙 Отмена"));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        telegramService.sendMessage(message);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_SETTINGS);
    }

    /* НОВЫЙ МЕТОД: Обработка выбора пакета токенов*/
    private void handleTokenPackageSelection(Long chatId, String text) {
        String tokenCount = "";
        String price = "";

        switch (text) {
            case "5 токенов - 25₽":
                tokenCount = "5";
                price = "25";
                break;
            case "10 токенов - 50₽":
                tokenCount = "10";
                price = "50";
                break;
            case "30 токенов - 150₽":
                tokenCount = "30";
                price = "150";
                break;
            case "50 токенов - 250₽":
                tokenCount = "50";
                price = "250";
                break;
            case "100 токенов - 500₽":
                tokenCount = "100";
                price = "500";
                break;
            case "🔙 Назад":
                showMainMenuCompact(chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                return;
            default:
                telegramService.sendMessage(chatId, "Неизвестный пакет");
                return;
        }

        /* Создаем платеж*/
        paymentHandler.handleTokenPackagePurchase(chatId, tokenCount, price);
        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
    }

    /**
     * Новый метод для обработки состояния загрузки нескольких фото
     */
    private boolean handleMultipleImagesUploadState(Long chatId, String text) {

        /* Сначала проверяем системные кнопки выхода*/

        if ("🏠 Главное меню".equals(text)) {
            stateManager.clearMultipleImages(chatId);
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            showMainMenuCompact(chatId);
            telegramService.sendMessage(chatId, "🏠 Возврат в главное меню.");
            return true;
        }

        if ("❌ Отмена слияния".equals(text)) {
            stateManager.clearMultipleImages(chatId);
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            showMainMenuCompact(chatId);
            telegramService.sendMessage(chatId, "❌ Слияние отменено.");
            return true;
        }

        /* Потом проверяем кнопку продолжения*/

        if ("✅ Все фото загружены, ввести промпт".equals(text)) {
            List<byte[]> images = stateManager.getMultipleImages(chatId);
            if (images != null && images.size() >= 2) {
                ImageConfig config = stateManager.getOrCreateConfig(chatId);
                config.setMode("merge");
                int tokensNeeded = costCalculatorService.calculateMergeTokens(config, images.size());
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MERGE_PROMPT);
                telegramService.sendMessage(chatId,
                        "✏️ Отлично! Загружено " + images.size() + " фото.\n\n" +
                                "💰 Будет списано: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                                "⚙️ Настройки: " + costCalculatorService.getDescription(config) + "\n\n" +
                                "Теперь введите описание для слияния:\n" +
                                "Пример: 'Наложи человека с фото 2 на фон фото 1 и добавь ему в руки ананас'\n\n" +
                                "⚠️ *Для отмены используйте 🏠 Главное меню или /start*"
                );
            } else {
                telegramService.sendMessage(chatId,
                        "❌ Нужно минимум 2 фото для слияния.\n" +
                                "Отправьте еще фото или нажмите /merge для начала заново."
                );
            }
            return true;
        }

        /* Если пользователь отправляет текст (не фото и не кнопку)*/

        if (!isMenuCommand(text)) {
            telegramService.sendMessage(chatId,
                    "📸 Я ожидаю загрузку фото для слияния.\n\n" +
                            "Отправьте фото или используйте кнопки:\n" +
                            "• ✅ Все фото загружены, ввести промпт\n" +
                            "• ❌ Отмена слияния\n" +
                            "• 🏠 Главное меню\n\n" +
                            "Или отправьте еще фото..."
            );
            return true;
        }

        return false;
    }

    /* НОВЫЙ МЕТОД: Обработка команды редактирования*/
    private void handleEditCommand(Long chatId) {
        if (!isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Проверяем баланс*/
        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        config.setMode("edit"); /* ← ДОБАВИТЬ ЭТУ СТРОЧКУ!*/
        int tokensNeeded = costCalculatorService.calculateTokens(config);

        if (!balanceService.hasEnoughTokens(user.getId(), tokensNeeded)) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно токенов!\n\n" +
                            "🎨 Баланс: " + balanceService.getTokensBalance(user.getId()) + " токенов\n" +
                            "💰 Требуется: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                            "🛒 Купите токены в магазине"
            );
            return;
        }

        /* После проверки баланса, перед установкой состояния:*/

        telegramService.sendMessage(chatId,
                "📸 *Загрузите изображение для редактирования:*\n\n" +
                        "🎨 Баланс: " + balanceService.getTokensBalance(user.getId()) + " токенов\n" +
                        "💰 Стоимость редактирования: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                        "⚙️ Текущие настройки: " + costCalculatorService.getDescription(config) + "\n\n" +
                        "Отправьте изображение, которое хотите изменить.\n" +
                        "После загрузки введите текстовое описание изменений."
        );

        /* Устанавливаем состояние ожидания загрузки фото*/
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_UPLOAD);
      /*  telegramService.sendMessage(chatId,
                "📸 *Загрузите изображение для редактирования:*\n\n" +
                        "Отправьте изображение, которое хотите изменить.\n" +
                        "После загрузки введите текстовое описание изменений."
        );*/
    }

    /* НОВЫЙ МЕТОД: Обработка ввода промпта для редактирования*/
    private void handleEditPromptInput(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Получаем загруженное изображение*/
        byte[] sourceImage = stateManager.getUploadedImage(chatId);
        if (sourceImage == null) {
            telegramService.sendMessage(chatId, "❌ Изображение не найдено. Попробуйте снова.");
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        /* Получаем настройки пользователя*/
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        /* Проверяем достаточно ли средств с учётом настроек качества*/
        int tokensNeeded = costCalculatorService.calculateTokens(config);
        if (!balanceService.canEditImage(user.getId(), config)) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно токенов!\n\n" +
                            "🎨 Баланс: " + balanceService.getTokensBalance(user.getId()) + " токенов\n" +
                            "💰 Требуется: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                            "🛒 Купите токены в магазине"
            );
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

       /* Списываем токены*/
        boolean used = balanceService.useImageEdit(user.getId(), config);
        if (!used) {
            telegramService.sendMessage(chatId, "❌ Ошибка списания баланса");
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        /* Меняем состояние и уведомляем*/
        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId,
                "🎨 Редактирую изображение...\n\n" +
                        "📝 Описание изменений: _" + prompt + "_\n" +
                        "⚙️ Настройки: " + costCalculatorService.getDescription(config) + "\n" +
                        "⏱️ Это займет ~ от 20 до 59 секунд"
        );

        /* Запускаем асинхронное редактирование*/
        startAsyncImageEdit(chatId, user.getId(), sourceImage, prompt, config);
    }

    /* НОВЫЙ МЕТОД: Команда настроек*/
    private void handleSettingsCommand(Long chatId) {
        if (!isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        /* Получаем текущие настройки*/
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        /* Устанавливаем mode для корректного отображения*/
        config.setMode("generate"); /* ← ДОБАВИТЬ!*/

        /* Отправляем меню настроек*/
        telegramService.sendMessage(chatId,
                "⚙️ *Настройки генерации*\n\n" +
                        "Текущие настройки:\n" +
                        "• Соотношение сторон: " + config.getAspectRatio() + "\n" +
                        "• Разрешение: " + config.getResolution() + "\n" +
                        "• Стоимость: " + costCalculatorService.getDescription(config) + "\n\n" +
                        "Выберите параметр для изменения:"
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setParseMode("Markdown");
        message.setText("Выберите параметр:"); /* <-- ВАЖНО: УСТАНОВИТЕ ТЕКСТ*/

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        /* Кнопки для изменения соотношения сторон*/
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📐 1:1 (Квадрат)"));
        row1.add(new KeyboardButton("📐 16:9 (Широкий)"));
        row1.add(new KeyboardButton("🎬 21:9 (Кино)"));
        row1.add(new KeyboardButton("🖥️ 4:3 (Классический)"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📱 9:16 (Сторис)"));
        row2.add(new KeyboardButton("📄 2:3 (Портрет)"));
        row2.add(new KeyboardButton("📷 3:2 (Фото)"));
        row2.add(new KeyboardButton("📱 3:4 (Смартфон)"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📄 4:5 (Вертикальный)"));
        row3.add(new KeyboardButton("📊 5:4 (Соотношение 5:4)"));

        /* Кнопки для изменения разрешения*/
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("🖼️ 1K (Базовое)"));
        row4.add(new KeyboardButton("🖼️ 2K (Качественное)"));
        row4.add(new KeyboardButton("🖼️ 4K (Максимальное)"));

        /* Кнопка назад*/
        KeyboardRow rowReturn = new KeyboardRow();
        rowReturn.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(rowReturn);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        telegramService.sendMessage(message);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_QUALITY_SETTINGS);
    }

    /* НОВЫЙ МЕТОД: Обработка выбора настроек качества*/
    private void handleQualitySettingsInput(Long chatId, String text) {

        /* ДОБАВЬТЕ ПРОВЕРКУ*/

        if (text == null) {
            log.error("handleQualitySettingsInput: text is null for chatId: {}", chatId);
            telegramService.sendMessage(chatId, "❌ Некорректный ввод");
            return;
        }

            /* ДОБАВИТЬ ПРОВЕРКУ - ЕСЛИ ТЕКСТ НЕ КНОПКА:*/

            if (!isQualitySettingsButton(text)) {
                telegramService.sendMessage(chatId,
                        "❌ Используйте кнопки для изменения настроек!\n\n" +
                                "Нажмите одну из кнопок ниже или:\n" +
                                "• 🔙 Назад - для выхода из настроек"
                );
                return;
            }

        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        boolean settingsChanged = false;

        switch (text) {
            case "📐 1:1 (Квадрат)":
                config.setAspectRatio("1:1");
                settingsChanged = true;
                break;
            case "📐 16:9 (Широкий)":
                config.setAspectRatio("16:9");
                settingsChanged = true;
                break;
            case "🎬 21:9 (Кино)":
                config.setAspectRatio("21:9");
                settingsChanged = true;
                break;
            case "🖥️ 4:3 (Классический)":
                config.setAspectRatio("4:3");
                settingsChanged = true;
                break;
            case "📱 9:16 (Сторис)":
                config.setAspectRatio("9:16");
                settingsChanged = true;
                break;
            case "📄 2:3 (Портрет)":
                config.setAspectRatio("2:3");
                settingsChanged = true;
                break;
            case "📷 3:2 (Фото)":
                config.setAspectRatio("3:2");
                settingsChanged = true;
                break;
            case "📱 3:4 (Смартфон)":
                config.setAspectRatio("3:4");
                settingsChanged = true;
                break;
            case "📄 4:5 (Вертикальный)":
                config.setAspectRatio("4:5");
                settingsChanged = true;
                break;
            case "📊 5:4 (Соотношение 5:4)":
                config.setAspectRatio("5:4");
                settingsChanged = true;
                break;
            case "🖼️ 1K (Базовое)":
                config.setResolution("1K");
                settingsChanged = true;
                break;
            case "🖼️ 2K (Качественное)":
                config.setResolution("2K");
                settingsChanged = true;
                break;
            case "🖼️ 4K (Максимальное)":
                config.setResolution("4K");
                settingsChanged = true;
                break;
            case "🔙 Назад":
                showMainMenuCompact(chatId);
                stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                return;
        }

        if (settingsChanged) {
            stateManager.saveConfig(chatId, config);
            telegramService.sendMessage(chatId,
                    "✅ Настройки обновлены!\n\n" +
                            "Новые параметры:\n" +
                            "• Соотношение сторон: " + config.getAspectRatio() + "\n" +
                            "• Разрешение: " + config.getResolution() + "\n" +
                            "• Стоимость: " + costCalculatorService.getDescription(config)
            );

            /* Снова показываем меню настроек*/
            handleSettingsCommand(chatId);
        }
    }

    /**
     * Проверяет, является ли текст кнопкой из меню настроек
     */
    private boolean isQualitySettingsButton(String text) {
        if (text == null) return false;

        return text.equals("📐 1:1 (Квадрат)") ||
                text.equals("📐 16:9 (Широкий)") ||
                text.equals("🎬 21:9 (Кино)") ||
                text.equals("🖥️ 4:3 (Классический)") ||
                text.equals("📱 9:16 (Сторис)") ||
                text.equals("📄 2:3 (Портрет)") ||
                text.equals("📷 3:2 (Фото)") ||
                text.equals("📱 3:4 (Смартфон)") ||
                text.equals("📄 4:5 (Вертикальный)") ||
                text.equals("📊 5:4 (Соотношение 5:4)") ||
                text.equals("🖼️ 1K (Базовое)") ||
                text.equals("🖼️ 2K (Качественное)") ||
                text.equals("🖼️ 4K (Максимальное)") ||
                text.equals("🔙 Назад");
    }

    /*СТАРЫЙ МЕТОД*/

    private void handleImagePackageSelection(Long chatId, String text) {
        String packageType = "image";
        String count;
        String price;

        switch (text) {
            case "3 генерации - 39₽":
                count = "3";
                price = "39";
                break;
            case "10 генераций - 99₽":
                count = "10";
                price = "99";
                break;
            case "50 генераций - 449₽":
                count = "50";
                price = "449";
                break;
            case "100 генераций - 799₽":
                count = "100";
                price = "799";
                break;
            case "300 генераций - 2099₽":
                count = "300";
                price = "2099";
                break;
            case "🔙 Назад":

                /* Возвращаем к выбору типа пакета*/

                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("🛒 *Покупка генераций*\n\nВыберите тип генераций:");
                message.setParseMode("Markdown");

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);

                List<KeyboardRow> rows = new ArrayList<>();

                KeyboardRow row1 = new KeyboardRow();
                row1.add(new KeyboardButton("🎨 Изображения"));
                row1.add(new KeyboardButton("🎥 Видео"));

                KeyboardRow row2 = new KeyboardRow();
                row2.add(new KeyboardButton("🔙 Назад"));

                rows.add(row1);
                rows.add(row2);
                keyboard.setKeyboard(rows);
                message.setReplyMarkup(keyboard);

                telegramService.sendMessage(message);
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_PACKAGE_TYPE);
                return;

            default:
                telegramService.sendMessage(chatId, "Неизвестный пакет");
                return;
        }

        /* Создаем платеж*/

        paymentHandler.handlePackagePurchase(chatId, packageType, count);
        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
    }

    /*СТАРЫЙ МЕТОД*/

    private void handleVideoPackageSelection(Long chatId, String text) {
        String packageType = "video";
        String count;

        switch (text) {
            case "1 видео - 50₽":
                count = "1";
                break;
            case "5 видео - 225₽":
                count = "5";
                break;
            case "10 видео - 399₽":
                count = "10";
                break;
            case "🔙 Назад":

                /* Аналогично возвращаем к выбору типа*/

                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText("🛒 *Покупка генераций*\n\nВыберите тип генераций:");
                message.setParseMode("Markdown");

                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);

                List<KeyboardRow> rows = new ArrayList<>();

                KeyboardRow row1 = new KeyboardRow();
                row1.add(new KeyboardButton("🎨 Изображения"));
                row1.add(new KeyboardButton("🎥 Видео"));

                KeyboardRow row2 = new KeyboardRow();
                row2.add(new KeyboardButton("🔙 Назад"));

                rows.add(row1);
                rows.add(row2);
                keyboard.setKeyboard(rows);
                message.setReplyMarkup(keyboard);

                telegramService.sendMessage(message);
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_PACKAGE_TYPE);
                return;

            default:
                telegramService.sendMessage(chatId, "Неизвестный пакет");
                return;
        }

        paymentHandler.handlePackagePurchase(chatId, packageType, count);
        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
    }

    private void handleCommand(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.User telegramUser) {

        /*if (text.equals("/start") || text.equals("🏠 Старт")) {*/

        if ("/start".equals(text) || "🏠 Старт".equals(text)) {
            handleStartCommand(chatId, telegramUser);
            return;
        }

        switch (text) {
            case "/register", "📝 Зарегистрироваться":
                authService.handleRegisterCommand(chatId);
                break;
            case "/login", "🔑 Войти":
                authService.handleLoginCommand(chatId);
                break;
            case "📋 Информация":
                sendInfoMenu(chatId);
                break;
            case "📞 Контакты":
                sendContactsMenu(chatId);
                break;
            case "✅ Проверить оплату":
                handleCheckPaymentCommand(chatId);
                break;

            case "/test_higgsfield":
                if (isUserAuthorized(chatId)) {
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_TEST_PROMPT);
                    telegramService.sendMessage(chatId, "Введите промпт для теста Higgsfield:");
                }
                break;

            default:
                handleAuthorizedCommand(chatId, text);
        }
    }

    private void handleStartCommand(Long chatId, org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        stateManager.clearUserData(chatId);

        /* Создаём в БД заглушку (user_123456) — только для id и баланса*/
        userService.findOrCreateByTelegramId(chatId);

        /* Имя берём из Telegram и используем только здесь*/
        String firstName = telegramUser.getFirstName() != null ? telegramUser.getFirstName() : "друг";

        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);

        /* Отправляем приветствие с Inline-кнопкой*/
        sendWelcomeWithInlineButton(chatId, firstName);

        telegramService.setMenuButton(chatId);

        showMainMenuCompact(chatId);

    }

    private void sendWelcomeWithInlineButton(Long chatId, String firstName) {
        String text = "👋 Добро пожаловать, " + firstName + "!\n\n" +
                "*Nano Banana* - это передовая нейросеть\n" +
                "для обработки и генерации фото!\n\n" +
                "Отправьте фото с описанием,или\n" +
                "альбом из фотографий с описанием\n" +
                "чтобы приступить к генерации 👇";

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("🚀 Приступить");
        button.setCallbackData("start_generation");

        row.add(button);
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(inlineKeyboard);

        telegramService.sendMessage(message);
    }

    @Override
    public void showMainMenuCompact(Long chatId) {
        telegramService.sendMessage(menuFactory.showMainMenuCompact(chatId));
        /*menuFactory.showMainMenuCompact(chatId);*/
    }

    private void handleAuthorizedCommand(Long chatId, String text) {
        if (!isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "Пожалуйста, авторизуйтесь: /login");
            return;
        }

        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        if ("🎨 Сгенерировать изображение".equals(text)) {
            handleImageGenerationCommand(chatId, user);
        } else if ("✏️ Редактировать изображение".equals(text)) {
            handleEditCommand(chatId);
        } else if ("⚙️ Настройки".equals(text)) {
            handleSettingsCommand(chatId);

        } else switch (text) {
            case "🎥 Сгенерировать видео" -> {
                int balance = balanceService.getVideoBalance(user.getId());
                if (balance > 0) {
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_VIDEO_PROMPT);
                    telegramService.sendMessage(chatId,
                            "🎥 *Введите описание для видео:*\n\n" +
                                    "Осталось видео: " + balance + "\n" +
                                    "Пример: 'Левитирующий остров с водопадом'"
                    );
                } else {
                    telegramService.sendMessage(chatId,
                            "❌ Недостаточно генераций видео!\n\n" +
                                    "🎥 Баланс: 0 видео\n" +
                                    "🛒 Купите пакет видео в магазине"
                    );
                }
            }
            case "🛒 Купить генерации" -> {
                telegramService.sendMessage(menuFactory.createTokenPackagesMenu(chatId));
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_TOKEN_PACKAGE);
            }
            case "📊 Мой баланс" -> telegramService.sendMessage(menuFactory.createStatsMenu(chatId));
            case "🔙 Назад", "🏠 Главное меню" ->  showMainMenuCompact(chatId);
            case "📋 Информация" -> sendInfoMenu(chatId);
            case "📞 Контакты" -> sendContactsMenu(chatId);
            case "❌ Выйти" -> authService.handleLogout(chatId);
            case "📋 Примеры промптов" ->
                    telegramService.sendMessage(menuFactory.createPromptsExamplesMenu(chatId));
            default -> telegramService.sendMessage(chatId, "Неизвестная команда");
        }
    }

    /* НОВЫЙ МЕТОД: Обработка команды генерации с учетом настроек*/

    private void handleImageGenerationCommand(Long chatId, User user) {
        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        config.setMode("generate"); /* ← ДОБАВИТЬ!*/
        int tokensNeeded = costCalculatorService.calculateTokens(config);
        int userBalance = balanceService.getTokensBalance(user.getId());

        if (balanceService.canGenerateImage(user.getId(), config)) {
            telegramService.sendMessage(chatId,
                    "🎨 *Введите описание для изображения:*\n\n" +
                            "🎨 Баланс: " + userBalance + " токенов\n" +
                            "💰 Будет списано: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                            "⚙️ Текущие настройки: " + costCalculatorService.getDescription(config) + "\n\n" +
                            "Пример: 'Космонавт верхом на лошади в стиле Пикассо'"
            );

            stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PROMPT);
        } else {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно токенов!\n\n" +
                            "🎨 Баланс: " + userBalance + " токенов\n" +
                            "💰 Требуется: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                            "🛒 Купите токены в магазине"
            );
        }
    }

    private boolean isMenuCommand(String text) {

        /* УБЕДИТЕСЬ, ЧТО text НЕ null*/
        if (text == null) return false;

        return text.equals("🎨 Сгенерировать изображение") ||
                text.equals("🎥 Сгенерировать видео") ||
                text.equals("🛒 Купить генерации") ||
                text.equals("📊 Мой баланс") ||
                text.equals("🔙 Назад") ||
                text.equals("🏠 Главное меню") ||
                text.equals("📋 Информация") ||
                text.equals("📞 Контакты") ||
                text.equals( "✏️ Редактировать изображение") || /* НОВОЕ*/
                text.equals("⚙️ Настройки") ||                 /* НОВОЕ*/
                text.equals("🖼️ Объединить изображения") ||  /* Новая команда*/
                text.equals("❌ Выйти") ||
                text.equals("📝 Зарегистрироваться") ||  /* ← ДОБАВИТЬ!*/
                text.equals("📋 Примеры промптов") ||
                text.equals("🔑 Войти");
    }

    /* НОВЫЙ МЕТОД: Асинхронное редактирование изображения*/

    @Async
    public void startAsyncImageEdit(Long chatId, Long userId, byte[] sourceImage,
                                    String prompt, ImageConfig config) {
        try {
            log.info("Начало редактирования через CometAPI для chatId: {}", chatId);

            /* Вызов API для редактирования*/
            byte[] imageBytes = cometApiService.editImage(sourceImage, prompt, config);
            int newBalance = balanceService.getTokensBalance(userId);

            /* Отправляем результат*/

            /*telegramService.sendPhoto(chatId, imageBytes, "edited_image.jpg");*/

            /* ★ Умная отправка для редактирования тоже*/

            telegramService.sendImageSmart(chatId, imageBytes, "edited_image.jpg", config);

            telegramService.sendMessage(chatId,
                    "✅ Изображение отредактировано!\n\n" +
                            "📝 Описание изменений: _" + prompt + "_\n" +
                            "⚙️ Настройки: " + costCalculatorService.getDescription(config) + "\n" +
                            "🎨 Осталось генераций: " + newBalance
            );

            log.info("Редактирование успешно для chatId: {}", chatId);

        } catch (Exception e) {
            log.error("Ошибка редактирования для chatId: {}", chatId, e);

            /* Возвращаем баланс при ошибке*/
            try {
                int tokens = costCalculatorService.calculateTokens(config);
                balanceService.refundTokens(userId, tokens);
                log.info("Баланс возвращен для userId: {}", userId);
            } catch (Exception ex) {
                log.error("Не удалось вернуть баланс для userId: {}", userId, ex);
            }

            telegramService.sendMessage(chatId,
                    "❌ Произошла ошибка при редактировании\n\n" +
                            "🎨 Баланс возвращен\n" +
                            "⚠️ Попробуйте позже или измените запрос"
            );
        } finally {

            /* Очищаем временное изображение*/

            stateManager.clearUserData(chatId);
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
        }
    }

    private void handleCheckPaymentCommand(Long chatId) {
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_PAYMENT_ID);
        telegramService.sendMessage(chatId, "Введите ID платежа из ЮKassa:");
    }

    @Override
    public boolean isUserAuthorized(Long chatId) {
        String state = stateManager.getUserState(chatId);
        User user = userService.findByTelegramChatId(chatId);

        return (UserStateManager.STATE_AUTHORIZED_MAIN.equals(state) ||
                UserStateManager.STATE_WAITING_IMAGE_PROMPT.equals(state) ||
                UserStateManager.STATE_WAITING_VIDEO_PROMPT.equals(state) ||
                UserStateManager.STATE_WAITING_PACKAGE_TYPE.equals(state) ||
                UserStateManager.STATE_WAITING_IMAGE_PACKAGE.equals(state) ||
                UserStateManager.STATE_WAITING_VIDEO_PACKAGE.equals(state) ||
                UserStateManager.STATE_REGISTER_EMAIL.equals(state) ||
                UserStateManager.STATE_REGISTER_USERNAME.equals(state) ||
                UserStateManager.STATE_REGISTER_PASSWORD.equals(state) ||

                /* ДОБАВЬТЕ ВСЕ НОВЫЕ СОСТОЯНИЯ:*/

                UserStateManager.STATE_WAITING_IMAGE_UPLOAD.equals(state) ||             /* Для загрузки фото*/
                UserStateManager.STATE_WAITING_EDIT_PROMPT.equals(state) ||              /* Для ввода промпта редактирования*/
                UserStateManager.STATE_WAITING_QUALITY_SETTINGS.equals(state) ||         /* Для настроек качества*/
                UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD.equals(state) ||
                UserStateManager.STATE_WAITING_MERGE_PROMPT.equals(state) ||
                UserStateManager.STATE_WAITING_TOKEN_PACKAGE.equals(state) ||            /* Добавить эту строку*/
                UserStateManager.STATE_WAITING_USER_INPUT.equals(state) ||               // ← ДОБАВИТЬ
                UserStateManager.STATE_GENERATION_IN_PROGRESS.equals(state)             /* Для генерации*/
        ) && user != null;
    }

    private boolean isFreeCommand(String text) {
        return List.of(
                "✅ Проверить оплату", "🔙 Назад", "🏠 Старт",
                "📝 Зарегистрироваться", "🔑 Войти", "❌ Выйти"
        ).contains(text);
    }

    private void sendWelcomeMenu(Long chatId) {
        telegramService.sendMessage(menuFactory.createWelcomeMenu(chatId));
    }

    private void sendMainMenu(Long chatId) {
        telegramService.sendMessage(menuFactory.createMainMenu(chatId));
    }

    private void sendInfoMenu(Long chatId) {
        telegramService.sendMessage(menuFactory.createInfoMenu(chatId));
    }

    private void sendContactsMenu(Long chatId) {
        telegramService.sendMessage(menuFactory.createContactsMenu(chatId));
    }

    @Override
    public void handleError(Update update, Exception exception) {
        log.error("Bot error processing update: {}", exception.getMessage());
        if (update.hasMessage()) {
            telegramService.sendMessage(update.getMessage().getChatId(),
                    "⚠️ Произошла системная ошибка. Попробуйте позже.");
        }
    }

    @Override
    public void shutdown() {
        log.info("MessageHandler shutting down...");
    }

    /* Новый метод для обработки команды merge*/

    private void handleMergeCommand(Long chatId) {
        if (!isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Проверяем баланс*/

        ImageConfig config = stateManager.getOrCreateConfig(chatId);
        config.setMode("merge");
        int minTokensNeeded = costCalculatorService.calculateMergeTokens(config, 2); /* Минимум 2 фото*/

        if (!balanceService.hasEnoughTokens(user.getId(), minTokensNeeded)) {
            int userBalance = balanceService.getTokensBalance(user.getId());
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно токенов!\n\n" +
                            "🎨 Баланс: " + userBalance + " токенов\n" +
                            "💰 Минимально требуется: " + minTokensNeeded + " токенов (" + (minTokensNeeded * 5) + " ₽)\n" +
                            "🛒 Купите токены в магазине"
            );
            return;
        }

        /* Устанавливаем состояние ожидания загрузки нескольких фото*/

        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_MULTIPLE_IMAGES_UPLOAD);
        stateManager.clearMultipleImages(chatId);

        telegramService.sendMessage(chatId,
                "🖼️ *Объединение нескольких изображений*\n\n" +
                        "🎨 Баланс: " + balanceService.getTokensBalance(user.getId()) + " токенов\n" +
                        "💰 Стоимость слияния 2 фото: " + minTokensNeeded + " токенов (" + (minTokensNeeded * 5) + " ₽)\n" +
                        "📸 Загрузите 2-8 изображений одним сообщением:\n" +
                        "1. Нажмите 'Добавить файл' в Telegram\n" +
                        "2. Выберите несколько изображений\n" +
                        "3. Нажмите 'Отправить'\n\n" +
                        "После загрузки введите описание того, как объединить изображения.\n" +
                        "Пример: 'Создай коллаж из этих фото в стиле ретро'"
        );
    }

    /* Новый метод для обработки промпта слияния*/

    private void handleMergePromptInput(Long chatId, String prompt) {

        /* Защита от дурака - если вдруг пришла системная кнопка*/

        if ("❌ Отмена слияния".equals(prompt) || "🏠 Главное меню".equals(prompt)) {
            stateManager.clearMultipleImages(chatId);
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            if ("🏠 Главное меню".equals(prompt)) {
                showMainMenuCompact(chatId);
            } else {
                telegramService.sendMessage(chatId, "❌ Слияние отменено.");
                showMainMenuCompact(chatId);
            }
            return;
        }

        /* НОВАЯ ЗАЩИТА: если пришла кнопка "ввести промпт" как текст*/

        if ("✅ Все фото загружены, ввести промпт".equals(prompt)) {
            telegramService.sendMessage(chatId,
                    "❌ Нельзя использовать кнопку как промпт!\n\n" +
                            "📝 Пожалуйста, введите текстовое описание того, как объединить фото.\n" +
                            "Пример: 'Создай коллаж из этих фото в стиле ретро'"
            );
            return; /* Остаёмся в том же состоянии, ждём правильный ввод*/
        }

        /* Проверка на пустой промпт или слишком короткий*/

        if (prompt == null || prompt.trim().length() < 3) {
            telegramService.sendMessage(chatId,
                    "❌ Промпт должен содержать минимум 3 символа.\n" +
                            "Пожалуйста, введите описание для слияния:"
            );
            return;
        }

        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        /* Получаем все загруженные изображения*/

        List<byte[]> images = stateManager.getMultipleImages(chatId);
        if (images == null || images.size() < 2) {
            telegramService.sendMessage(chatId,
                    "❌ Загружено недостаточно изображений (нужно минимум 2).\n" +
                            "Попробуйте снова: /merge"
            );
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        /* Проверяем лимит (CometAPI поддерживает до 8-14 изображений)*/

        if (images.size() > 8) {
            telegramService.sendMessage(chatId,
                    "⚠️ Загружено слишком много изображений (" + images.size() + ").\n" +
                            "Использую первые 8 изображений."
            );
            images = images.subList(0, Math.min(8, images.size()));
        }

        /* Получаем настройки пользователя*/

        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        /* Проверяем достаточно ли средств
        double cost = config.calculateCost();*/

        config.setMode("merge");
        int tokensNeeded = costCalculatorService.calculateMergeTokens(config, images.size());
        if (!balanceService.canMergeImages(user.getId(), config, images.size())) {
            int userBalance = balanceService.getTokensBalance(user.getId());
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно токенов!\n\n" +
                            "🎨 Баланс: " + userBalance + " токенов\n" +
                            "💰 Требуется: " + tokensNeeded + " токенов (" + (tokensNeeded * 5) + " ₽)\n" +
                            "🛒 Купите токены в магазине"
            );
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        /* Списываем токены*/

        boolean used = balanceService.useImageMerge(user.getId(), config, images.size());
        if (!used) {
            telegramService.sendMessage(chatId, "❌ Ошибка списания баланса");
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        /* Меняем состояние и уведомляем*/

        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId,
                "🖼️ Объединяю " + images.size() + " изображений...\n\n" +
                        "📝 Описание: _" + prompt + "_\n" +
                        "⚙️ Настройки: " + costCalculatorService.getDescription(config) + "\n" +
                        "⏱️ Это займет ~от 30 до 59 секунд"
        );

        /* Запускаем асинхронное слияние*/

        startAsyncImageMerge(chatId, user.getId(), images, prompt, config);
    }

    /* Новый асинхронный метод для слияния*/

    @Async
    public void startAsyncImageMerge(Long chatId, Long userId, List<byte[]> images,
                                     String prompt, ImageConfig config) {
        try {
            log.info("Начало слияния {} изображений через CometAPI для chatId: {}",
                    images.size(), chatId);

            /* Вызов API для слияния*/

            byte[] resultImage = cometApiService.mergeImages(images, prompt, config);
            int newBalance = balanceService.getTokensBalance(userId);

            /* Отправляем результат*/

            telegramService.sendImageSmart(chatId, resultImage, "merged_image.jpg", config);

            telegramService.sendMessage(chatId,
                    "✅ Изображения успешно объединены!\n\n" +
                            "📝 Описание: _" + prompt + "_\n" +
                            "🖼️ Объединено фото: " + images.size() + "\n" +
                            "⚙️ Настройки: " + costCalculatorService.getDescription(config) + "\n" +
                            "🎨 Осталось генераций: " + newBalance
            );

            log.info("Слияние успешно для chatId: {}", chatId);

        } catch (Exception e) {
            log.error("Ошибка слияния для chatId: {}", chatId, e);

            /* Возвращаем баланс при ошибке*/

            try {
                int tokens = costCalculatorService.calculateMergeTokens(config, images.size());
                balanceService.refundTokens(userId, tokens);
                log.info("Баланс возвращен для userId: {}", userId);
            } catch (Exception ex) {
                log.error("Не удалось вернуть баланс для userId: {}", userId, ex);
            }

            telegramService.sendMessage(chatId,
                    "❌ Произошла ошибка при объединении изображений\n\n" +
                            "🎨 Баланс возвращен\n" +
                            "⚠️ Попробуйте позже или используйте другое описание"
            );
        } finally {
            stateManager.clearUserData(chatId);
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
        }
    }

}



