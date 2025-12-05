package org.example.nanobananaprobot.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nanobananaprobot.bot.keyboards.MenuFactory;
import org.example.nanobananaprobot.bot.service.*;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.GenerationBalanceService;
import org.example.nanobananaprobot.service.UserServiceData;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        String userState = stateManager.getUserState(chatId);

        log.debug("Handling message - ChatId: {}, Text: {}, State: {}", chatId, text, userState);

        try {
            /* ГЛОБАЛЬНЫЕ КОМАНДЫ*/
            if (text.equals("/start") || text.equals("🏠 Старт")) {
                handleStartCommand(chatId);
                return;
            }

            /* ГЛОБАЛЬНЫЕ КНОПКИ МЕНЮ*/
            if (text.equals("🔙 Назад") || text.equals("🏠 Главное меню")) {
                if (isUserAuthorized(chatId)) {
                    sendMainMenu(chatId);
                    stateManager.setUserState(chatId, UserStateManager.STATE_AUTHORIZED_MAIN);
                } else {
                    sendWelcomeMenu(chatId);
                    stateManager.setUserState(chatId, UserStateManager.STATE_NONE);
                }
                return;
            }

            /* Обработка состояний ввода*/
            if (handleInputStates(chatId, text, userState)) {
                return;
            }

            /* Обработка команд*/
            handleCommand(chatId, text);

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            telegramService.sendMessage(chatId, "❌ Произошла ошибка. Попробуйте еще раз.");
        }
    }

    private boolean handleInputStates(Long chatId, String text, String userState) {
        /* БЛОКИРОВКА КНОПОК ВО ВРЕМЯ ВВОДА*/
        if (userState.equals(UserStateManager.STATE_WAITING_IMAGE_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_VIDEO_PROMPT) ||
                userState.equals(UserStateManager.STATE_WAITING_USERNAME) ||
                userState.equals(UserStateManager.STATE_WAITING_PASSWORD) ||
                userState.equals(UserStateManager.STATE_REGISTER_USERNAME) ||
                userState.equals(UserStateManager.STATE_REGISTER_PASSWORD) ||
                userState.equals(UserStateManager.STATE_REGISTER_EMAIL) ||
                userState.equals(UserStateManager.STATE_WAITING_PAYMENT_ID)) {

            if (isMenuCommand(text)) {
                telegramService.sendMessage(chatId, "❌ Завершите текущий процесс ввода");
                return true;
            }
        }

        if (text.equals("❌ Выйти")) {
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

            default:
                return false;
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
        if (text.equals("/start") || text.equals("🏠 Старт")) {
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
        if ("🎨 Сгенерировать изображение".equals(text)) {
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

        } else if ("🎥 Сгенерировать видео".equals(text)) {
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

        } else if ("🛒 Купить генерации".equals(text)) {
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

        } else if ("📊 Мой баланс".equals(text)) {
            telegramService.sendMessage(menuFactory.createStatsMenu(chatId));

        } else if ("🔙 Назад".equals(text)) {
            sendMainMenu(chatId);

        } else if ("🏠 Главное меню".equals(text)) {
            sendMainMenu(chatId);

        } else if ("📋 Информация".equals(text)) {
            sendInfoMenu(chatId);

        } else if ("📞 Контакты".equals(text)) {
            sendContactsMenu(chatId);

        } else if ("❌ Выйти".equals(text)) {
            authService.handleLogout(chatId);

        } else {
            telegramService.sendMessage(chatId, "Неизвестная команда");
        }
    }

    private boolean isMenuCommand(String text) {
        return text.equals("🎨 Сгенерировать изображение") ||
                text.equals("🎥 Сгенерировать видео") ||
                text.equals("🛒 Купить генерации") ||
                text.equals("📊 Мой баланс") ||
                text.equals("🔙 Назад") ||
                text.equals("🏠 Главное меню") ||
                text.equals("📋 Информация") ||
                text.equals("📞 Контакты") ||
                text.equals("❌ Выйти");
    }

    private void handleCheckPaymentCommand(Long chatId) {
        stateManager.setUserState(chatId, UserStateManager.STATE_WAITING_PAYMENT_ID);
        telegramService.sendMessage(chatId, "Введите ID платежа из ЮKassa:");
    }

    private boolean isUserAuthorized(Long chatId) {
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
                UserStateManager.STATE_REGISTER_PASSWORD.equals(state)
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



