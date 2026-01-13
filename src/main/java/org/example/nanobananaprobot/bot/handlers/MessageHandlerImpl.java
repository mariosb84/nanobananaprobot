package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.keyboards.MenuFactory;
import org.example.nanobananaprobot.bot.service.*;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.CometApiService;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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

    @Override
    public void handleTextMessage(Message message) {

        // 1. ПЕРЕНЕСИТЕ ЭТУ ПРОВЕРКУ В САМОЕ НАЧАЛО МЕТОДА
        if (message == null || message.getText() == null) {
            log.debug("Ignoring non-text message from chatId: {}",
                    message != null ? message.getChatId() : "N/A");
            return;
        }

        Long chatId = message.getChatId();
        String text = message.getText();

        // 🔴 ПЕРЕМЕСТИТЕ try-catch БЛОК СЮДА - сразу после получения chatId и text
        try {
            String userState = stateManager.getUserState(chatId);
            log.debug("Handling message - ChatId: {}, Text: {}, State: {}", chatId, text, userState);

            /* ДОБАВЛЯЕМ НОВЫЕ ГЛОБАЛЬНЫЕ КОМАНДЫ*/
            switch (text) {
                case "/settings", "⚙️ Настройки" -> {
                    handleSettingsCommand(chatId);
                    return;
                }
                case "/edit", "✏️ Редактировать изображение" -> {
                    handleEditCommand(chatId);
                    return;
                }


                /* ГЛОБАЛЬНЫЕ КОМАНДЫ*/
                case "/start", "🏠 Старт" -> {
                    handleStartCommand(chatId);
                    return;
                }


                /* ГЛОБАЛЬНЫЕ КНОПКИ МЕНЮ*/
                case "🔙 Назад", "🏠 Главное меню" -> {
                    if (isUserAuthorized(chatId)) {
                        sendMainMenu(chatId);
                        stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                    } else {
                        sendWelcomeMenu(chatId);
                        stateManager.setUserState(chatId, UserStateManager.STATE_NONE);
                    }
                    return;
                }
            }

            /* Обработка состояний ввода*/
            if (handleInputStates(chatId, text, userState)) {
                return;
            }

            /* Обработка команд*/
            handleCommand(chatId, text);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            log.error("Error handling message:", e); // <-- ВАЖНО: передать сам объект 'e'
            telegramService.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте еще раз.");
        }
    }

    private boolean handleInputStates(Long chatId, String text, String userState) {

        // ДОБАВЬТЕ ЭТУ ПРОВЕРКУ
        if (text == null) {
            log.error("handleInputStates received NULL text! ChatId: {}, State: {}", chatId, userState);
            return true; // Или false, в зависимости от логики
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
                userState.equals(UserStateManager.STATE_WAITING_QUALITY_SETTINGS)

        ) {

            if (isMenuCommand(text)) {
                telegramService.sendMessage(chatId, "❌ Завершите текущий процесс ввода");
                return true;
            }
        }

        //if (text.equals("❌ Выйти")) {

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

            /* ОБРАБОТКА ВЫБОРА ПАКЕТОВ*/
            case UserStateManager.STATE_WAITING_PACKAGE_TYPE:
                if ("🎨 Изображения".equals(text)) {
                    telegramService.sendMessage(menuFactory.createImagePackagesMenu(chatId));
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PACKAGE);
                } else if ("🎥 Видео".equals(text)) {
                    telegramService.sendMessage(menuFactory.createVideoPackagesMenu(chatId));
                    stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_VIDEO_PACKAGE);
                } else {
                    sendMainMenu(chatId);
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

            case UserStateManager.STATE_WAITING_EDIT_PROMPT:
                handleEditPromptInput(chatId, text);
                return true;

            case UserStateManager.STATE_WAITING_QUALITY_SETTINGS:
                handleQualitySettingsInput(chatId, text);
                return true;

            default:
                return false;
        }
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

        // Проверяем баланс
        if (balanceService.getImageBalance(user.getId()) <= 0) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций!\n\n" +
                            "🎨 Баланс: 0 изображений\n" +
                            "🛒 Купите пакет генераций в магазине"
            );
            return;
        }

        // Устанавливаем состояние ожидания загрузки фото
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_UPLOAD);
        telegramService.sendMessage(chatId,
                "📸 *Загрузите изображение для редактирования:*\n\n" +
                        "Отправьте изображение, которое хотите изменить.\n" +
                        "После загрузки введите текстовое описание изменений."
        );
    }

    /* НОВЫЙ МЕТОД: Обработка ввода промпта для редактирования*/
    private void handleEditPromptInput(Long chatId, String prompt) {
        User user = userService.findByTelegramChatId(chatId);
        if (user == null) {
            telegramService.sendMessage(chatId, "❌ Пользователь не найден");
            return;
        }

        // Получаем загруженное изображение
        byte[] sourceImage = stateManager.getUploadedImage(chatId);
        if (sourceImage == null) {
            telegramService.sendMessage(chatId, "❌ Изображение не найдено. Попробуйте снова.");
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        // Получаем настройки пользователя
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        // Проверяем достаточно ли средств с учётом настроек качества
        double cost = config.calculateCost();
        if (!balanceService.canAffordGeneration(user.getId(), cost)) {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно средств!\n\n" +
                            "💰 Требуется: $" + String.format("%.2f", cost) + "\n" +
                            "🛒 Пополните баланс"
            );
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        // Списываем баланс
        boolean used = balanceService.useImageGeneration(user.getId(), cost);
        if (!used) {
            telegramService.sendMessage(chatId, "❌ Ошибка списания баланса");
            stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
            return;
        }

        // Меняем состояние и уведомляем
        stateManager.setUserState(chatId, UserStateManager.STATE_GENERATION_IN_PROGRESS);

        telegramService.sendMessage(chatId,
                "🎨 Редактирую изображение...\n\n" +
                        "📝 Описание изменений: _" + prompt + "_\n" +
                        "⚙️ Настройки: " + config.getDescription() + "\n" +
                        "⏱️ Это займет ~20 секунд"
        );

        // Запускаем асинхронное редактирование
        startAsyncImageEdit(chatId, user.getId(), sourceImage, prompt, config);
    }

    /* НОВЫЙ МЕТОД: Команда настроек*/
    private void handleSettingsCommand(Long chatId) {
        if (!isUserAuthorized(chatId)) {
            telegramService.sendMessage(chatId, "❌ Пожалуйста, авторизуйтесь: /login");
            return;
        }

        // Получаем текущие настройки
        ImageConfig config = stateManager.getOrCreateConfig(chatId);

        // Отправляем меню настроек
        telegramService.sendMessage(chatId,
                "⚙️ *Настройки генерации*\n\n" +
                        "Текущие настройки:\n" +
                        "• Соотношение сторон: " + config.getAspectRatio() + "\n" +
                        "• Разрешение: " + config.getResolution() + "\n" +
                        "• Стоимость: $" + String.format("%.2f", config.calculateCost()) + "\n\n" +
                        "Выберите параметр для изменения:"
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setParseMode("Markdown");
        message.setText("Выберите параметр:"); // <-- ВАЖНО: УСТАНОВИТЕ ТЕКСТ

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Кнопки для изменения соотношения сторон
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📐 1:1 (Квадрат)"));
        row1.add(new KeyboardButton("📐 16:9 (Широкий)"));

        // Кнопки для изменения разрешения
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🖼️ 1K (Базовое)"));
        row2.add(new KeyboardButton("🖼️ 2K (Качественное)"));
        row2.add(new KeyboardButton("🖼️ 4K (Максимальное)"));

        // Кнопка назад
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔙 Назад"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        telegramService.sendMessage(message);
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_QUALITY_SETTINGS);
    }

    /* НОВЫЙ МЕТОД: Обработка выбора настроек качества*/
    private void handleQualitySettingsInput(Long chatId, String text) {

        // ДОБАВЬТЕ ПРОВЕРКУ
        if (text == null) {
            log.error("handleQualitySettingsInput: text is null for chatId: {}", chatId);
            telegramService.sendMessage(chatId, "❌ Некорректный ввод");
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
                sendMainMenu(chatId);
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
                            "• Стоимость: $" + String.format("%.2f", config.calculateCost())
            );

            // Снова показываем меню настроек
            handleSettingsCommand(chatId);
        }
    }

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

    private void handleCommand(Long chatId, String text) {

        //if (text.equals("/start") || text.equals("🏠 Старт")) {

        if ("/start".equals(text) || "🏠 Старт".equals(text)) {
            handleStartCommand(chatId);
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

    private void handleStartCommand(Long chatId) {
        stateManager.clearUserData(chatId);

        if (isUserAuthorized(chatId)) {
            sendMainMenu(chatId);
        } else {
            sendWelcomeMenu(chatId);
        }
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

        /* Проверяем команды*/

         /* if ("🎨 Сгенерировать изображение".equals(text)) {
            int balance = balanceService.getImageBalance(user.getId());
            if (balance > 0) {
                stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PROMPT);
                telegramService.sendMessage(chatId,
                        "🎨 *Введите описание для изображения:*\n\n" +
                                "Осталось генераций: " + balance + "\n" +
                                "Пример: 'Космонавт верхом на лошади в стиле Пикассо'"
                );
            } else {
                telegramService.sendMessage(chatId,
                        "❌ Недостаточно генераций!\n\n" +
                                "🎨 Баланс: 0 изображений\n" +
                                "🛒 Купите пакет генераций в магазине"
                );
            }

        }*/

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
            }
            case "📊 Мой баланс" -> telegramService.sendMessage(menuFactory.createStatsMenu(chatId));
            case "🔙 Назад", "🏠 Главное меню" -> sendMainMenu(chatId);
            case "📋 Информация" -> sendInfoMenu(chatId);
            case "📞 Контакты" -> sendContactsMenu(chatId);
            case "❌ Выйти" -> authService.handleLogout(chatId);
            default -> telegramService.sendMessage(chatId, "Неизвестная команда");
        }
    }

    /* НОВЫЙ МЕТОД: Обработка команды генерации с учетом настроек*/
    private void handleImageGenerationCommand(Long chatId, User user) {
        int balance = balanceService.getImageBalance(user.getId());
        if (balance > 0) {
            // Получаем настройки пользователя
            ImageConfig config = stateManager.getOrCreateConfig(chatId);

            telegramService.sendMessage(chatId,
                    "🎨 *Введите описание для изображения:*\n\n" +
                            "Осталось генераций: " + balance + "\n" +
                            "⚙️ Текущие настройки: " + config.getDescription() + "\n\n" +
                            "Пример: 'Космонавт верхом на лошади в стиле Пикассо'"
            );

            stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_IMAGE_PROMPT);
        } else {
            telegramService.sendMessage(chatId,
                    "❌ Недостаточно генераций!\n\n" +
                            "🎨 Баланс: 0 изображений\n" +
                            "🛒 Купите пакет генераций в магазине"
            );
        }
    }

    private boolean isMenuCommand(String text) {

        // УБЕДИТЕСЬ, ЧТО text НЕ null
        if (text == null) return false;

        return text.equals("🎨 Сгенерировать изображение") ||
                text.equals("🎥 Сгенерировать видео") ||
                text.equals("🛒 Купить генерации") ||
                text.equals("📊 Мой баланс") ||
                text.equals("🔙 Назад") ||
                text.equals("🏠 Главное меню") ||
                text.equals("📋 Информация") ||
                text.equals("📞 Контакты") ||
                text.equals( "✏️ Редактировать изображение") || // НОВОЕ
                text.equals("⚙️ Настройки") ||                 // НОВОЕ
                text.equals("❌ Выйти");
    }

    /* НОВЫЙ МЕТОД: Асинхронное редактирование изображения*/
    @Async
    public void startAsyncImageEdit(Long chatId, Long userId, byte[] sourceImage,
                                    String prompt, ImageConfig config) {
        try {
            log.info("Начало редактирования через CometAPI для chatId: {}", chatId);

            // Вызов API для редактирования
            byte[] imageBytes = cometApiService.editImage(sourceImage, prompt, config);
            int newBalance = balanceService.getImageBalance(userId);

            // Отправляем результат
            telegramService.sendPhoto(chatId, imageBytes, "edited_image.jpg");

            telegramService.sendMessage(chatId,
                    "✅ Изображение отредактировано!\n\n" +
                            "📝 Описание изменений: _" + prompt + "_\n" +
                            "⚙️ Настройки: " + config.getDescription() + "\n" +
                            "🎨 Осталось генераций: " + newBalance
            );

            log.info("Редактирование успешно для chatId: {}", chatId);

        } catch (Exception e) {
            log.error("Ошибка редактирования для chatId: {}", chatId, e);

            // Возвращаем баланс при ошибке
            try {
                double cost = config.calculateCost();
                balanceService.refundGeneration(userId, cost);
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
            // Очищаем временное изображение
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
                // ДОБАВЬТЕ ВСЕ НОВЫЕ СОСТОЯНИЯ:
                UserStateManager.STATE_WAITING_IMAGE_UPLOAD.equals(state) ||      // Для загрузки фото
                UserStateManager.STATE_WAITING_EDIT_PROMPT.equals(state) ||       // Для ввода промпта редактирования
                UserStateManager.STATE_WAITING_QUALITY_SETTINGS.equals(state) ||  // Для настроек качества
                UserStateManager.STATE_GENERATION_IN_PROGRESS.equals(state)       // Для генерации
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

}



